package com.gohmega.pct;

import com.threed.jpct.*;
import com.threed.jpct.util.KeyMapper;
import com.threed.jpct.util.KeyState;
import com.threed.jpct.util.Light;
import com.threed.jpct.util.ShadowHelper;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.event.KeyEvent;

public class LeMain implements IPaintListener
{
    private static final long serialVersionUID = 1L;

    private static float PI = (float) Math.PI;

    private KeyMapper keyMapper = null;
    private MouseMapper mouseMapper = null;

    private FrameBuffer buffer = null;

    private World world = null;
    private World sky = null;

    private Object3D plane = null;
    private Object3D puk = null;

    private Light sun = null;

    private ShadowHelper sh = null;
    private Projector projector = null;

    private int fps = 0;

    private boolean forward = false;
    private boolean backward = false;
    private boolean up = false;
    private boolean down = false;
    private boolean left = false;
    private boolean right = false;

    private float ind = 0;
    private float xAngle = 0;
    private boolean doLoop = true;
    private long time = System.currentTimeMillis();
    private Ticker ticker = new Ticker(15);

    public static void main(String[] args) throws Exception
    {
        Config.glVerbose = true;
        LeMain cd = new LeMain();
        cd.init();
        cd.gameLoop();
    }

    public LeMain()
    {
        Config.glAvoidTextureCopies = true;
        Config.maxPolysVisible = 1000;
        Config.glColorDepth = 24;
        Config.glFullscreen = false;
        Config.farPlane = 4000;
        Config.glShadowZBias = 0.8f;
        Config.lightMul = 1;
        Config.collideOffset = 500;
        Config.glTrilinear = true;
    }

    public void finishedPainting()
    {
        fps++;
    }

    public void startPainting()
    {
    }

    private void init() throws Exception
    {
        TextureManager tm = TextureManager.getInstance();
        tm.addTexture("metal", new Texture("res/metal.jpg"));
        tm.addTexture("flo", new Texture("res/fadeflo.jpg"));
        tm.addTexture("grass", new Texture("res/GrassSample2.jpg"));
        tm.addTexture("disco", new Texture("res/disco.jpg"));
        tm.addTexture("rock", new Texture("res/rock.jpg"));
        tm.addTexture("normals", new Texture("res/normals.jpg"));
        tm.addTexture("sky", new Texture("res/sky.jpg"));
        tm.addTexture("cloth", new Texture("res/cloth.jpg"));
        tm.addTexture("solid", new Texture("res/solid.jpg"));
        tm.addTexture("metric.tile", new Texture("res/metric.tile.jpg"));

        // Initialize frame buffer

        buffer = new FrameBuffer(640, 480, FrameBuffer.SAMPLINGMODE_NORMAL);
        buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
        buffer.enableRenderer(IRenderer.RENDERER_OPENGL, IRenderer.MODE_OPENGL);
        buffer.setPaintListener(this);

        // Initialize worlds

        world = new World();
        sky = new World();
        world.setAmbientLight(30, 30, 30);
        sky.setAmbientLight(255, 255, 255);

        world.getLights().setRGBScale(Lights.RGB_SCALE_2X);
        sky.getLights().setRGBScale(Lights.RGB_SCALE_2X);

        // Initialize mappers

        keyMapper = new KeyMapper();
        mouseMapper = new MouseMapper(buffer);
        mouseMapper.hide();

        // Load/create and setup objects

        final int planeSize = 32;
        plane = Primitives.getPlane(planeSize, 1);
        plane.rotateX(PI / 2f);
        plane.setSpecularLighting(true);
        plane.setTexture("metric.tile");
        //plane.setTexture("grass");
        //plane.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
        tileTexture(plane, planeSize);

        puk = Primitives.getCylinder(360, 1.0f, 0.2f);
        puk.setEnvmapped(Object3D.ENVMAP_ENABLED);
//        TextureInfo stoneTex = new TextureInfo(tm.getTextureID("rock"));
//        stoneTex.add(tm.getTextureID("normals"), TextureInfo.MODE_MODULATE);
//        puk.setTexture(stoneTex);
        puk.setTexture("solid");
        puk.setSpecularLighting(true);

        Object3D[] marks = placeMarkers();

        // Add objects to the worlds

        world.addObject(plane);
        world.addObject(puk);
        world.addObjects(marks);

        // Build all world's objects

        world.buildAllObjects();

        // Compile all objects for better performance

        plane.compileAndStrip();
        puk.compileAndStrip();
        for (int i = 0; i < marks.length; i++)
        {
            Object3D mark = marks[i];
            mark.compileAndStrip();
        }

        // Initialize shadow helper

        projector = new Projector();
        projector.setFOV(1.5f);
        projector.setYFOV(1.5f);

        sh = new ShadowHelper(world, buffer, projector, 2048);
        sh.setCullingMode(false);
        sh.setAmbientLight(new Color(30, 30, 30));
        sh.setLightMode(true);
        sh.setBorder(1);

        sh.addCaster(puk);
        sh.addReceiver(plane);

        // Setup dynamic light source

        sun = new Light(world);
        sun.setIntensity(250, 250, 250);
        sun.setAttenuation(800);

//        String fragmentShader = Loader.loadTextFile("res/shader/fragmentshader.glsl");
//        String vertexShader = Loader.loadTextFile("res/shader/vertexshader.glsl");
//
//        GLSLShader shader = new GLSLShader(vertexShader, fragmentShader);
//        shader.setShadowHelper(sh);
//        shader.setStaticUniform("colorMap", 0);
//        shader.setStaticUniform("normalMap", 1);
//        shader.setStaticUniform("invRadius", 0.0005f);
//        puk.setRenderHook(shader);

        // Move camera

        Camera cam = world.getCamera();
        cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
        cam.moveCamera(Camera.CAMERA_MOVEUP, 40);

        cam.lookAt(plane.getTransformedCenter());
        cam.setFOV(1.5f);

    }

    private Object3D[] placeMarkers()
    {
        final int cx = 3;
        final int cy = 1;
        final int cz = 3;
        final int total = cx * cy * cz;
        int idx = 0;
        Object3D markers[] = new Object3D[total];
        for(int x = 0; x < cx; x++)
        {
            for(int y = 0; y < cy; y++)
            {
                for(int z = 0; z < cz; z++)
                {
                    Object3D marker = Primitives.getCylinder(360, 0.2f, 2f);
                    marker.setEnvmapped(Object3D.ENVMAP_ENABLED);
//        TextureInfo stoneTex = new TextureInfo(tm.getTextureID("rock"));
//        stoneTex.add(tm.getTextureID("normals"), TextureInfo.MODE_MODULATE);
//        marker.setTexture(stoneTex);
                    marker.setTexture("solid");
                    marker.setSpecularLighting(true);
                    marker.translate(x - 1, y, z - 1);
                    markers[idx++] = marker;
                }
            }
        }
        return markers;
    }

    private void pollControls()
    {

        KeyState ks;
        while ((ks = keyMapper.poll()) != KeyState.NONE)
        {
            switch (ks.getKeyCode())
            {
                case KeyEvent.VK_ESCAPE:
                    doLoop = false;
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    forward = ks.getState();
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    backward = ks.getState();
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    left = ks.getState();
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    right = ks.getState();
                    break;
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_Q:
                    up = ks.getState();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_E:
                    down = ks.getState();
                    break;
                default:
                    break;
            }
        }

        if (org.lwjgl.opengl.Display.isCloseRequested())
        {
            doLoop = false;
        }
    }


    private void move(long ticks)
    {

        if (ticks == 0)
        {
            return;
        }

        // Key controls

        SimpleVector ellipsoid = new SimpleVector(5, 5, 5);

        if (forward)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVEIN, ellipsoid, ticks, 5);
        }

        if (backward)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVEOUT, ellipsoid, ticks, 5);
        }

        if (left)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVELEFT, ellipsoid, ticks, 5);
        }

        if (right)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVERIGHT, ellipsoid, ticks, 5);
        }

        if (up)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVEUP, ellipsoid, ticks, 5);
        }

        if (down)
        {
            world.checkCameraCollisionEllipsoid(Camera.CAMERA_MOVEDOWN, ellipsoid, ticks, 5);
        }

        // mouse rotation

        Matrix rot = world.getCamera().getBack();
        int dx = mouseMapper.getDeltaX();
        int dy = mouseMapper.getDeltaY();

        float ts = 0.2f * ticks;
        float tsy = ts;

        if (dx != 0)
        {
            ts = dx / 500f;
        }
        if (dy != 0)
        {
            tsy = dy / 500f;
        }

        if (dx != 0)
        {
            rot.rotateAxis(rot.getYAxis(), ts);
        }

        if ((dy > 0 && xAngle < Math.PI / 4.2) || (dy < 0 && xAngle > -Math.PI / 4.2))
        {
            rot.rotateX(tsy);
            xAngle += tsy;
        }

        // Update the skydome

        sky.getCamera().setBack(world.getCamera().getBack().cloneMatrix());
        //dome.rotateY(0.00005f * ticks);
    }

//    GLFont glFont = GLFont.getGLFont(new java.awt.Font("Dialog", Font.PLAIN, 12));
    private void gameLoop() throws Exception
    {

        //SimpleVector pos = snork.getTransformedCenter();
        SimpleVector pos = plane.getTransformedCenter();
        SimpleVector offset = new SimpleVector(1, 0, -1).normalize();

        long ticks;

        while (doLoop)
        {

            ticks = ticker.getTicks();
            if (ticks > 0)
            {
                // animate the snork and the dome

                animate(ticks);
                offset.rotateY(0.007f * ticks);

                // move the camera

                pollControls();
                move(ticks);
            }

            // update the projector for the shadow map

            projector.lookAt(plane.getTransformedCenter());
            projector.setPosition(pos);
            projector.moveCamera(new SimpleVector(0, -1, 0), 200);
//            projector.moveCamera(new SimpleVector(0, -1, 0), 200);
//            projector.moveCamera(offset, 215);
            sun.setPosition(projector.getPosition());

            // update the shadow map

            sh.updateShadowMap();

            // render the scene

            buffer.clear();

            buffer.setPaintListenerState(false);
            sky.renderScene(buffer);
            sky.draw(buffer);
            buffer.setPaintListenerState(true);
            sh.drawScene();
            buffer.update();
            buffer.displayGLOnly();

            // print out the fps to the console

            int lastFps = 0;
            if (System.currentTimeMillis() - time >= 1000)
            {
                System.out.println(fps);
                lastFps = fps;
                fps = 0;
                time = System.currentTimeMillis();
            }
        }
//        buffer.update();
//        glFont.blitString(frameBuffer, "this is a blitted text", x, y, Color.ORANGE);
//        buffer.display(null);

        // exit...

        System.exit(0);
    }

    int cLoops = 0;
    private void animate(long ticks)
    {
        if (ticks > 0)
        {

//            float ft = (float) ticks;
//            ind += 0.02f * ft;
//            if (ind > 1)
//            {
//                ind -= 1;
//            }
//            float deg = ((float) cLoops) / 360.0f;
//            puk.translate(-60.0f * (float) Math.cos((deg) * PI) / 360.0f, -60.0f * (float) Math.sin((deg) * PI) / 360.0f, 60.0f * (float) Math.cos((deg) * PI) / 360.0f);
//            cLoops++;
//            snork.animate(ind, 2);
//            snork.rotateY(-0.02f * ft);
//            snork.translate(0, -50, 0);
//            SimpleVector dir = snork.getXAxis();
//            dir.scalarMul(ft);
//            dir = snork.checkForCollisionEllipsoid(dir, new SimpleVector(5, 20, 5), 5);
//            snork.translate(dir);
//            dir = snork.checkForCollisionEllipsoid(new SimpleVector(0, 100, 0), new SimpleVector(5, 20, 5), 1);
//            snork.translate(dir);
        }
    }

    private void tileTexture(Object3D obj, float tileFactor)
    {
        PolygonManager pm = obj.getPolygonManager();

        int end = pm.getMaxPolygonID();
        for (int i = 0; i < end; i++)
        {
            SimpleVector uv0 = pm.getTextureUV(i, 0);
            SimpleVector uv1 = pm.getTextureUV(i, 1);
            SimpleVector uv2 = pm.getTextureUV(i, 2);

            uv0.scalarMul(tileFactor);
            uv1.scalarMul(tileFactor);
            uv2.scalarMul(tileFactor);

            int id = pm.getPolygonTexture(i);

            TextureInfo ti = new TextureInfo(id, uv0.x, uv0.y, uv1.x, uv1.y, uv2.x, uv2.y);
            pm.setPolygonTexture(i, ti);
        }
    }


    private static class MouseMapper
    {

        private boolean hidden = false;

        private int height = 0;

        public MouseMapper(FrameBuffer buffer)
        {
            height = buffer.getOutputHeight();
            init();
        }

        public void hide()
        {
            if (!hidden)
            {
                Mouse.setGrabbed(true);
                hidden = true;
            }
        }

        public void show()
        {
            if (hidden)
            {
                Mouse.setGrabbed(false);
                hidden = false;
            }
        }

        public boolean isVisible()
        {
            return !hidden;
        }

        public void destroy()
        {
            show();
            if (Mouse.isCreated())
            {
                Mouse.destroy();
            }
        }

        public boolean buttonDown(int button)
        {
            return Mouse.isButtonDown(button);
        }

        public int getMouseX()
        {
            return Mouse.getX();
        }

        public int getMouseY()
        {
            return height - Mouse.getY();
        }

        public int getDeltaX()
        {
            if (Mouse.isGrabbed())
            {
                return Mouse.getDX();
            }
            else
            {
                return 0;
            }
        }

        public int getDeltaY()
        {
            if (Mouse.isGrabbed())
            {
                return Mouse.getDY();
            }
            else
            {
                return 0;
            }
        }

        private void init()
        {
            try
            {
                if (!Mouse.isCreated())
                {
                    Mouse.create();
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static class Ticker
    {

        private int rate;
        private long s2;

        public static long getTime()
        {
            return System.currentTimeMillis();
        }

        public Ticker(int tickrateMS)
        {
            rate = tickrateMS;
            s2 = Ticker.getTime();
        }

        public int getTicks()
        {
            long i = Ticker.getTime();
            if (i - s2 > rate)
            {
                int ticks = (int) ((i - s2) / (long) rate);
                s2 += (long) rate * ticks;
                return ticks;
            }
            return 0;
        }
    }
}
