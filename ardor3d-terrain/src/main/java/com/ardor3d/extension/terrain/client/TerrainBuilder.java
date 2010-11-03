
package com.ardor3d.extension.terrain.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.ardor3d.extension.terrain.util.BresenhamYUpGridTracer;
import com.ardor3d.extension.terrain.util.TerrainGridCachePanel;
import com.ardor3d.extension.terrain.util.TextureGridCachePanel;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.google.common.collect.Lists;

public class TerrainBuilder {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TerrainBuilder.class.getName());

    public static int MAX_PICK_CHECKS = 500;

    private final TerrainDataProvider terrainDataProvider;
    private final Camera camera;

    private int clipmapTerrainCount = 20;
    private int clipmapTerrainSize = 127; // pow2 - 1
    private int clipmapTextureCount = 20;
    private int clipmapTextureSize = 128;

    private boolean showDebugPanels = false;

    private final List<TextureSource> extraTextureSources = Lists.newArrayList();

    public TerrainBuilder(final TerrainDataProvider terrainDataProvider, final Camera camera) {
        this.terrainDataProvider = terrainDataProvider;
        this.camera = camera;
    }

    public void addTextureConnection(final TextureSource textureSource) {
        extraTextureSources.add(textureSource);
    }

    public Terrain build() throws Exception {
        final Map<Integer, String> availableMaps = terrainDataProvider.getAvailableMaps();
        final int mapId = availableMaps.keySet().iterator().next();

        final TerrainSource terrainSource = terrainDataProvider.getTerrainSource(mapId);
        final Terrain terrain = buildTerrainSystem(terrainSource);

        final TextureSource textureSource = terrainDataProvider.getTextureSource(mapId);
        if (textureSource != null) {
            terrain.addTextureClipmap(buildTextureSystem(textureSource));

            for (final TextureSource extraSource : extraTextureSources) {
                terrain.addTextureClipmap(buildTextureSystem(extraSource));
            }
        }

        return terrain;
    }

    private Terrain buildTerrainSystem(final TerrainSource terrainSource) throws Exception {
        final TerrainConfiguration terrainConfiguration = terrainSource.getConfiguration();
        logger.info(terrainConfiguration.toString());

        final int clipmapLevels = terrainConfiguration.getTotalNrClipmapLevels();
        final int clipLevelCount = Math.min(clipmapLevels, clipmapTerrainCount);

        final int tileSize = terrainConfiguration.getCacheGridSize();

        int cacheSize = (clipmapTerrainSize + 1) / tileSize + 4;
        cacheSize += cacheSize & 1 ^ 1;

        logger.info("server clipmapLevels: " + clipmapLevels);

        final List<TerrainCache> cacheList = Lists.newArrayList();
        TerrainCache parentCache = null;

        final int baseLevel = Math.max(clipmapLevels - clipLevelCount, 0);
        int level = clipLevelCount - 1;

        logger.info("baseLevel: " + baseLevel);
        logger.info("level: " + level);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TerrainCache gridCache = new TerrainGridCache(parentCache, cacheSize, terrainSource, tileSize,
                    clipmapTerrainSize, terrainConfiguration, level--, i);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        final Terrain terrain = new Terrain(camera, cacheList, clipmapTerrainSize, terrainConfiguration);

        terrain.makePickable(BresenhamYUpGridTracer.class, MAX_PICK_CHECKS, new Vector3(1, 0, 1));

        logger.info("client clipmapLevels: " + cacheList.size());

        if (showDebugPanels) {
            final TerrainGridCachePanel panel = new TerrainGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Terrain Cache Debug");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 10, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return terrain;
    }

    private TextureClipmap buildTextureSystem(final TextureSource textureSource) throws Exception {
        final TextureConfiguration textureConfiguration = textureSource.getConfiguration();
        logger.info(textureConfiguration.toString());

        final int clipmapLevels = textureConfiguration.getTotalNrClipmapLevels();
        final int textureClipLevelCount = Math.min(clipmapLevels, clipmapTextureCount);

        final int tileSize = textureConfiguration.getCacheGridSize();

        int cacheSize = (clipmapTextureSize + 1) / tileSize + 4;
        cacheSize += cacheSize & 1 ^ 1;

        logger.info("server clipmapLevels: " + clipmapLevels);

        final List<TextureCache> cacheList = Lists.newArrayList();
        TextureCache parentCache = null;
        final int baseLevel = Math.max(clipmapLevels - textureClipLevelCount, 0);
        int level = textureClipLevelCount - 1;

        logger.info("baseLevel: " + baseLevel);
        logger.info("level: " + level);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TextureCache gridCache = new TextureGridCache(parentCache, cacheSize, textureSource, tileSize,
                    clipmapTextureSize, textureConfiguration, level--, i);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        logger.info("client clipmapLevels: " + cacheList.size());

        final TextureClipmap textureClipmap = new TextureClipmap(cacheList, clipmapTextureSize, textureConfiguration);

        if (showDebugPanels) {
            final TextureGridCachePanel panel = new TextureGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Texture Cache Debug");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 120, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return textureClipmap;
    }

    public TerrainBuilder setClipmapTerrainCount(final int clipmapTerrainCount) {
        this.clipmapTerrainCount = clipmapTerrainCount;
        return this;
    }

    public TerrainBuilder setClipmapTerrainSize(final int clipmapTerrainSize) {
        this.clipmapTerrainSize = clipmapTerrainSize;
        return this;
    }

    public TerrainBuilder setClipmapTextureCount(final int clipmapTextureCount) {
        this.clipmapTextureCount = clipmapTextureCount;
        return this;
    }

    public TerrainBuilder setClipmapTextureSize(final int clipmapTextureSize) {
        this.clipmapTextureSize = clipmapTextureSize;
        return this;
    }

    public TerrainBuilder setShowDebugPanels(final boolean showDebugPanels) {
        this.showDebugPanels = showDebugPanels;
        return this;
    }
}
