package cs5625.deferred.apps;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.Timer;

import cs5625.deferred.physics.ParticleSystem;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.rendering.Renderer;
import cs5625.deferred.scenegraph.SceneObject;
import cs5625.deferred.ui.MainViewWindow;

/**
 * SceneController.java
 * 
 * The SceneController class contains the application main() method. It's responsible for creating 
 * the OpenGL window and renderer, and its subclasses are responsible for handling user actions.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-23
 */
public abstract class SceneController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, ActionListener
{
	/*
	 * Member variables to store the renderer, scene root, and camera.
	 */
	protected MainViewWindow mMainWindow;
	protected Renderer mRenderer = null;
	protected SceneObject mSceneRoot = new SceneObject();
	protected Camera mCamera = new Camera();
	protected Camera mShadowCamera = new Camera();
	
	protected boolean hasShadows = false;
	
	/*
	 * Shadow mode state. Put in here for convenience
	 */
	protected boolean isShadowCamMode = false;
	protected boolean moveShadowCam = false;
	
	@SuppressWarnings("unused")
	private static SceneController globalController = null;
	
	protected static Timer timer;
	protected boolean isAnimate = false;
	private static int N_STEPS_PER_FRAME = 60;
	public static  float DT = 0.03f;
	public static float dt = 2 *  DT /  N_STEPS_PER_FRAME ;
	
	public ParticleSystem PS;
	
	/**
	 * SceneController contains the application main() method. It creates an OpenGL 
	 * window and renderer and a default controller instance to manage the scene.
	 * 
	 * @param args Unused.
	 */
	public static void main(String[] args)
	{
		/*
		 * There is some weird jar conflict issue on OS X loading the Point2i class, 
		 * so force it to load the right one.
		 */
		/*try
		{
			Class.forName("javax.vecmath.Point2i");
		}
		catch (ClassNotFoundException err)
		{
			err.printStackTrace();
			System.exit(-1);
		}*/

		/* 
		 * Create the scene controller instance. If you want a different style of 
		 * control (e.g. game-style input), make a new subclass!
		 */

		//globalController = new DefaultSceneController();
		//globalController = new ManyLightsSceneController();
		//globalController = new MaterialTestSceneController();
		//globalController = new TexturesTestSceneController();
		//globalController = new ShadowMapSceneController();
		//globalController = new DesertTreeController();
		//globalController = new SandDuneSceneController();
		globalController = new TreeSceneController();

	}
	
	/*
	 * Default constructor initializes the OpenGL window. 
	 */
	public SceneController()
	{
		mMainWindow = new MainViewWindow("CS 5625 Deferred Renderer", this);
		mMainWindow.setVisible(true);
        timer = new Timer( (int) (1000 *  DT), this);
	}
	
	/**
	 * Called once on initialization to create the default scene. Subclasses must implement this method.
	 */
	public abstract void initializeScene();
	
	/**
	 * Can be called by anyone to request a re-render.
	 */
	public void requiresRender()
	{
		mMainWindow.repaint();
	}
	
	/**
	 * Can be called by anyone to tell a self-animating controller to update and render a new frame.
	 * Default implementation just calls `mSceneRoot.animate(dt)` and `requiresRender()`.
	 * 
	 * @param dt The time (in seconds) since the last frame update. Used for time-based (as opposed to 
	 *        frame-based) animation.
	 */
	public void nextFrame(float DT)
	{
		//Initialize PS if necessary
		if (PS == null){
	        PS = new ParticleSystem();
	        mSceneRoot.addToParticleSystem(PS);
		}

		//Perform several small time steps per iteration
		//float dt = DT / N_STEPS_PER_FRAME ;
		for(int i =0; i< N_STEPS_PER_FRAME; i++){
			PS.advanceTime(dt);
		}
		mSceneRoot.animate(DT);
		requiresRender();
		
	}
	
	/**
	 * Called by the OpenGL view to re-render the scene.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void renderGL(GLAutoDrawable drawable)
	{
		mRenderer.render(drawable, mSceneRoot, isShadowCamMode ? mShadowCamera : mCamera, !isShadowCamMode && hasShadows ? mShadowCamera : null);
	}

	/**
	 * Called once after the OpenGL context has been set up to perform one-time initialization. 
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void initGL(GLAutoDrawable drawable)
	{
		mRenderer = new Renderer();
		mRenderer.init(drawable);
		mShadowCamera.setIsShadowMapCamera(true);
		initializeScene();
	}
	
	/**
	 * Called once before the OpenGL context will be destroyed to free all context-related resources. 
	 * This implementation sets the camera, scene, and renderer to null. This ensures that all textures, 
	 * shaders, FBOs, and any other resources are no longer in use.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void disposeGL(GLAutoDrawable drawable)
	{
		GL2 gl = drawable.getGL().getGL2();
		mCamera.releaseGPUResources(gl);
		mSceneRoot.releaseGPUResources(gl);
		mRenderer.releaseGPUResources(gl);
	}

	/**
	 * Called by the OpenGL view to notify the renderer that its dimensions have changed.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 * @param width The new width (in pixels) of the context.
	 * @param height The new height (in pixels) of the context.
	 */
	public void resizeGL(GLAutoDrawable drawable, int width, int height)
	{
		mRenderer.resize(drawable, width, height);
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void keyPressed(KeyEvent key)
	{
		moveShadowCam = key.isShiftDown();
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void keyReleased(KeyEvent key)
	{
		moveShadowCam = key.isShiftDown();
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 * By default, the following keys will control the renderer in the specified way:
	 * '1', ..., '6': Specifies a gbuffer texture to preview.
	 * '7': Visualize the normals.
	 * '8': Visualize the tangents, only works for anisotropic ward objects.
	 * '9': Visualize the bitangents, only works for anisotropic ward objects.
	 * '0': Stop displaying a gbuffer texture or visualization.
	 * 't': Toggle toon shading.
	 * 'w': Toggle wireframes.
	 * 'b': Toggle bloom post-processing.
	 * 'v'/'V': Decrease/Increase the bloom variance.
	 * 'c'/'C': Decrease/Increase the bloom threshold.
	 * 'x'/'X': Decrease/Increase the bloom width.
	 */
	@Override
	public void keyTyped(KeyEvent key)
	{
		/* Responds to the default renderer display requests. */
		char c = key.getKeyChar();

		if (c >= '0' && c <= '9')
		{
			mRenderer.previewGBuffer(c - '0' - 1);
			requiresRender();
		}
		else if (c == 't')
		{
			mRenderer.setToonShading(!mRenderer.getToonShading());
			requiresRender();
		}
		else if (c == 'w')
		{
			mRenderer.setRenderWireframes(!mRenderer.getRenderWireframes());
			requiresRender();
		}
		else if (c == 'c')
		{
			mRenderer.setBloomThreshold(Math.max(0.0f, mRenderer.getBloomThreshold() - 0.025f));
			System.out.println("Bloom Threshold: " + mRenderer.getBloomThreshold());
			requiresRender();
		}
		else if (c == 'C')
		{
			mRenderer.setBloomThreshold(mRenderer.getBloomThreshold() + 0.025f);
			System.out.println("Bloom Threshold: " + mRenderer.getBloomThreshold());
			requiresRender();
		}
		else if (c == 'x')
		{
			mRenderer.setBloomWidth(Math.max(0, mRenderer.getBloomWidth() - 1));
			System.out.println("Bloom Width: " + mRenderer.getBloomWidth());
			requiresRender();
		}
		else if (c == 'X')
		{
			mRenderer.setBloomWidth(mRenderer.getBloomWidth() + 1);
			System.out.println("Bloom Width: " + mRenderer.getBloomWidth());
			requiresRender();
		}
		else if (c == 's' && hasShadows) {
			isShadowCamMode = !isShadowCamMode;
			mShadowCamera.setIsShadowMapCamera(!mShadowCamera.getIsShadowMapCamera());
			System.out.println("Now controlling the " + (isShadowCamMode ? "shadow" : "main") + " camera");
			requiresRender();
		}
		else if (c == 'd') {
			mRenderer.setShadowMapBias(mRenderer.getShadowMapBias() - 0.000001f);
			System.out.println("Shadow Map Bias: " + mRenderer.getShadowMapBias());
			requiresRender();
		}
		else if (c == 'D') {
			mRenderer.setShadowMapBias(mRenderer.getShadowMapBias() + 0.000001f);
			System.out.println("Shadow Map Bias: " + mRenderer.getShadowMapBias());
			requiresRender();
		}
		else if (c == 'a') {
			mRenderer.incrementShadowMode();
			int mode = mRenderer.getShadowMode();
			String modeString = (mode == 0 ? "DEFAULT SHADOWMAP" : (mode == 1 ? "PCF SHADOWMAP" : "PCSS SHADOWMAP"));
			System.out.println("Shadow Map mode: " + modeString);
			requiresRender();
		}
		else if (c == 'f') {
			if (mRenderer.getShadowMode() == 1) {
				mRenderer.setShadowSampleWidth(mRenderer.getShadowSampleWidth() - 1);
				System.out.println("Shadow Map Sample Width: " + mRenderer.getShadowSampleWidth());
				requiresRender();
			}
			if (mRenderer.getShadowMode() == 2) {
				mRenderer.setLightWidth(mRenderer.getLightWidth() - 1);
				System.out.println("Shadow Map Light Width: " + mRenderer.getLightWidth());
				requiresRender();
			}
		}
		else if (c == 'F') {
			if (mRenderer.getShadowMode() == 1) {
				mRenderer.setShadowSampleWidth(mRenderer.getShadowSampleWidth() + 1);
				System.out.println("Shadow Map Sample Width: " + mRenderer.getShadowSampleWidth());
				requiresRender();
			}
			if (mRenderer.getShadowMode() == 2) {
				mRenderer.setLightWidth(mRenderer.getLightWidth() + 1);
				System.out.println("Shadow Map Light Width: " + mRenderer.getLightWidth());
				requiresRender();
			}
		}
		else if (c == 'S')
		{
			mRenderer.setSSAOEnabled(!mRenderer.getSSAOEnabled());
			System.out.println("SSAO enabled: " + mRenderer.getSSAOEnabled());
			requiresRender();
		}
		else if (c == 'r')
		{
			mRenderer.setSSAORadius(Math.max(mRenderer.getSSAORadius() - 0.01f, 0.01f));
			System.out.println("SSAO radius: " + mRenderer.getSSAORadius());
			requiresRender();
		}
		else if (c == 'R')
		{
			mRenderer.setSSAORadius(mRenderer.getSSAORadius() + 0.01f);
			System.out.println("SSAO radius: " + mRenderer.getSSAORadius());
			requiresRender();
		}
		else if (c == 'e')
		{
			int min = 5;
			int max = mRenderer.getMaxSSAORays();
			
			mRenderer.createNewSSAORays(Math.max(min, Math.min(max, mRenderer.getSSAORayCount() - 5)));
			System.out.println("SSAO rays: " + mRenderer.getSSAORayCount());
			requiresRender();
		}
		else if (c == 'E')
		{
			int min = 5;
			int max = mRenderer.getMaxSSAORays();
			
			mRenderer.createNewSSAORays(Math.max(min, Math.min(max, mRenderer.getSSAORayCount() + 5)));
			System.out.println("SSAO rays: " + mRenderer.getSSAORayCount());
			requiresRender();
		}
		else if (c == ' ')
		{
			isAnimate = !isAnimate;
			if (isAnimate){
				startAnimation();
			}
			else{
				stopAnimation();
			}
		}
		else if (c == '.'){
			mRenderer.setFog(!mRenderer.getFog());
			requiresRender();
		}
		
		else if (c == 'q')
		{
			mRenderer.setFogThreshold(Math.max(0.0f, mRenderer.getFogThreshold() - 0.25f));
			System.out.println("Fog Threshold: " + mRenderer.getFogThreshold());
			requiresRender();
		}
		else if (c == 'Q')
		{
			mRenderer.setFogThreshold(mRenderer.getFogThreshold() + 0.25f);
			System.out.println("Fog Threshold: " + mRenderer.getFogThreshold());
			requiresRender();
		}
		
//		else if (c == '.')
//		{
//			PS = new ParticleSystem();
//			mSceneRoot = new SceneObject();
//			isAnimate = false;
//			timer = new Timer( (int) (1000 *  DT), this);
//			globalController = new SandDuneSceneController();
//		}
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseDragged(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseClicked(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseEntered(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseExited(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mousePressed(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		/* No default response. */
	}
	
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == timer)
		{
			this.nextFrame(DT);
		}
	}
	
	public void startAnimation() {
		timer.start();
	}
	
	public void stopAnimation() {
		timer.stop();		
	}
}
