package com.gmail.nossr50.util.blockmeta.chunkmeta;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.Boolean;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.runnables.ChunkletUnloader;
import com.gmail.nossr50.runnables.blockstoreconversion.BlockStoreConversionZDirectory;
import com.gmail.nossr50.util.blockmeta.ChunkletStore;
import com.gmail.nossr50.util.blockmeta.PrimitiveChunkletStore;
import com.gmail.nossr50.util.blockmeta.PrimitiveExChunkletStore;
import com.gmail.nossr50.util.blockmeta.HashChunkletManager;

import org.getspout.spoutapi.chunkstore.mcMMOSimpleRegionFile;

public class HashChunkManager implements ChunkManager {
    private HashMap<UUID, HashMap<Long, mcMMOSimpleRegionFile>> regionFiles = new HashMap<UUID, HashMap<Long, mcMMOSimpleRegionFile>>();
    public HashMap<String, ChunkStore> store = new HashMap<String, ChunkStore>();
    public ArrayList<BlockStoreConversionZDirectory> converters = new ArrayList<BlockStoreConversionZDirectory>();
    private HashMap<UUID, Boolean> oldData = new HashMap<UUID, Boolean>();

    @Override
    public synchronized void closeAll() {
        for (UUID uid : regionFiles.keySet()) {
            HashMap<Long, mcMMOSimpleRegionFile> worldRegions = regionFiles.get(uid);
            Iterator<mcMMOSimpleRegionFile> itr = worldRegions.values().iterator();
            while (itr.hasNext()) {
                mcMMOSimpleRegionFile rf = itr.next();
                if (rf != null) {
                    rf.close();
                    itr.remove();
                }
            }
        }
        regionFiles.clear();
    }

    @Override
    public synchronized ChunkStore readChunkStore(World world, int x, int z) throws IOException {
        mcMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z);
        InputStream in = rf.getInputStream(x, z);
        if (in == null) {
            return null;
        }
        ObjectInputStream objectStream = new ObjectInputStream(in);
        try {
            Object o = objectStream.readObject();
            if (o instanceof ChunkStore) {
                return (ChunkStore) o;
            } else {
                throw new RuntimeException("Wrong class type read for chunk meta data for " + x + ", " + z);
            }
        } catch (IOException e) {
            // Assume the format changed
            return null;
            //throw new RuntimeException("Unable to process chunk meta data for " + x + ", " + z, e);
        } catch (ClassNotFoundException e) {
            // Assume the format changed
            //System.out.println("[SpoutPlugin] is Unable to find serialized class for " + x + ", " + z + ", " + e.getMessage());
            return null;
            //throw new RuntimeException("Unable to find serialized class for " + x + ", " + z, e);
        }
    }

    @Override
    public synchronized void writeChunkStore(World world, int x, int z, ChunkStore data) {
        if (!data.isDirty()) {
            return;
        }
        try {
            mcMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z);
            ObjectOutputStream objectStream = new ObjectOutputStream(rf.getOutputStream(x, z));
            objectStream.writeObject(data);
            objectStream.flush();
            objectStream.close();
            data.setDirty(false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write chunk meta data for " + x + ", " + z, e);
        }
    }

    @Override
    public synchronized void closeChunkStore(World world, int x, int z) {
        mcMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z);
        if (rf != null) {
            rf.close();
        }
    }

    private synchronized mcMMOSimpleRegionFile getSimpleRegionFile(World world, int x, int z) {
        File directory = new File(world.getWorldFolder(), "mcmmo_regions");

        directory.mkdirs();

        UUID key = world.getUID();

        HashMap<Long, mcMMOSimpleRegionFile> worldRegions = regionFiles.get(key);

        if (worldRegions == null) {
            worldRegions = new HashMap<Long, mcMMOSimpleRegionFile>();
            regionFiles.put(key, worldRegions);
        }

        int rx = x >> 5;
        int rz = z >> 5;

        long key2 = (((long) rx) << 32) | (((long) rz) & 0xFFFFFFFFL);

        mcMMOSimpleRegionFile regionFile = worldRegions.get(key2);

        if (regionFile == null) {
            File file = new File(directory, "mcmmo_" + rx + "_" + rz + "_.mcm");
            regionFile = new mcMMOSimpleRegionFile(file, rx, rz);
            worldRegions.put(key2, regionFile);
        }

        return regionFile;
    }

    @Override
    public synchronized void loadChunklet(int cx, int cy, int cz, World world) {
        loadChunk(cx, cz, world);
    }

    @Override
    public synchronized void unloadChunklet(int cx, int cy, int cz, World world) {
        unloadChunk(cx, cz, world);
    }

    @Override
    public synchronized void loadChunk(int cx, int cz, World world) {
        if(world == null)
            return;

        if(store.containsKey(world.getName() + "," + cx + "," + cz))
            return;

        ChunkStore in = null;

        UUID key = world.getUID();
        if(!this.oldData.containsKey(key))
            this.oldData.put(key, (new File(world.getWorldFolder(), "mcmmo_data")).exists());

        if(this.oldData.containsKey(key) && oldData.get(key))
            convertChunk(new File(world.getWorldFolder(), "mcmmo_data"), cx, cz, world, true);

        try {
            in = readChunkStore(world, cx, cz);
        }
        catch(Exception e) {}

        if(in != null) {
            store.put(world.getName() + "," + cx + "," + cz, in);
        }
    }

    @Override
    public synchronized void unloadChunk(int cx, int cz, World world) {
        saveChunk(cx, cz, world);

        if(store.containsKey(world.getName() + "," + cx + "," + cz)) {
            store.remove(world.getName() + "," + cx + "," + cz);
        }
    }

    @Override
    public synchronized void saveChunk(int cx, int cz, World world) {
        if(world == null)
            return;

        if(store.containsKey(world.getName() + "," + cx + "," + cz)) {
            ChunkStore out = store.get(world.getName() + "," + cx + "," + cz);

            if(!out.isDirty())
                return;

            writeChunkStore(world, cx, cz, out);
        }
    }

    @Override
    public synchronized boolean isChunkLoaded(int cx, int cz, World world) {
        if(world == null)
            return false;

        return store.containsKey(world.getName() + "," + cx + "," + cz);
    }

    @Override
    public synchronized void chunkLoaded(int cx, int cz, World world) {}

    @Override
    public synchronized void chunkUnloaded(int cx, int cz, World world) {
        if(world == null)
            return;

        ChunkletUnloader.addToList(cx, cz, world);
    }

    @Override
    public synchronized void saveWorld(World world) {
        if(world == null)
            return;

        closeAll();
        String worldName = world.getName();

        for(String key : store.keySet()) {
            String[] info = key.split(",");
            if(worldName.equals(info[0])) {
                int cx = 0;
                int cz = 0;

                try {
                    cx = Integer.parseInt(info[1]);
		    cz = Integer.parseInt(info[2]);
                }
		catch(Exception e) {
                    return;
                }
                saveChunk(cx, cz, world);
            }
        }
    }

    @Override
    public synchronized void unloadWorld(World world) {
        if(world == null)
            return;

        closeAll();
        String worldName = world.getName();

        for(String key : store.keySet()) {
            String[] info = key.split(",");
            if(worldName.equals(info[0])) {
                int cx = 0;
                int cz = 0;

                try {
                    cx = Integer.parseInt(info[1]);
		    cz = Integer.parseInt(info[2]);
                }
		catch(Exception e) {
                    return;
                }
                unloadChunk(cx, cz, world);
            }
        }
    }

    @Override
    public synchronized void loadWorld(World world) {}

    @Override
    public synchronized void saveAll() {
        closeAll();

        for(World world : Bukkit.getWorlds()) {
            saveWorld(world);
        }
    }

    @Override
    public synchronized void unloadAll() {
        closeAll();

        for(World world : Bukkit.getWorlds()) {
            unloadWorld(world);
        }
    }

    @Override
    public synchronized boolean isTrue(int x, int y, int z, World world) {
        if(world == null)
            return false;

        int cx = x / 16;
        int cz = z / 16;
        String key = world.getName() + "," + cx + "," + cz;

        if (!store.containsKey(key)) {
            loadChunk(cx, cz, world);
        }

        if (!store.containsKey(key)) {
            return false;
        }

        ChunkStore check = store.get(key);
        int ix = Math.abs(x) % 16;
        int iz = Math.abs(z) % 16;

        return check.isTrue(ix, y, iz);
    }

    @Override
    public synchronized boolean isTrue(Block block) {
        if(block == null)
            return false;

        return isTrue(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public synchronized void setTrue(int x, int y, int z, World world) {
        if(world == null)
            return;

        int cx = x / 16;
        int cz = z / 16;

        int ix = Math.abs(x) % 16;
        int iz = Math.abs(z) % 16;

        String key = world.getName() + "," + cx + "," + cz;

        if (!store.containsKey(key)) {
            loadChunk(cx, cz, world);
        }

        ChunkStore cStore = store.get(key);

        if (cStore == null) {
            cStore = ChunkStoreFactory.getChunkStore(world, cx, cz);
            store.put(key, cStore);
        }

        cStore.setTrue(ix, y, iz);
    }

    @Override
    public synchronized void setTrue(Block block) {
        if(block == null)
            return;

        setTrue(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public synchronized void setFalse(int x, int y, int z, World world) {
        if(world == null)
            return;

        int cx = x / 16;
        int cz = z / 16;

        int ix = Math.abs(x) % 16;
        int iz = Math.abs(z) % 16;

        String key = world.getName() + "," + cx + "," + cz;

        if (!store.containsKey(key)) {
            loadChunk(cx, cz, world);
        }

        ChunkStore cStore = store.get(key);

        if (cStore == null) {
            return; //No need to make a store for something we will be setting to false
        }

        cStore.setFalse(ix, y, iz);
    }

    @Override
    public synchronized void setFalse(Block block) {
        if(block == null)
            return;

        setFalse(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public synchronized void cleanUp() {}

    public synchronized void convertChunk(File dataDir, int cx, int cz, World world) {
        convertChunk(dataDir, cx, cz, world, false);
    }

    public synchronized void convertChunk(File dataDir, int cx, int cz, World world, boolean actually) {
        if(!actually)
            return;
        if(!dataDir.exists()) return;
        File cxDir = new File(dataDir, "" + cx);
        if(!cxDir.exists()) return;
        File czDir = new File(cxDir, "" + cz);
        if(!czDir.exists()) return;

        boolean conversionSet = false;

        for(BlockStoreConversionZDirectory converter : this.converters) {
            if(converter == null)
                continue;

            if(converter.taskID >= 0)
                continue;

            converter.start(world, cxDir, czDir);
            conversionSet = true;
            break;
        }

        if(!conversionSet) {
            BlockStoreConversionZDirectory converter = new BlockStoreConversionZDirectory();
            converter.start(world, cxDir, czDir);
            converters.add(converter);
        }
    }
}
