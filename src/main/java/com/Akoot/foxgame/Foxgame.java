package com.Akoot.foxgame;

import static org.lwjgl.glfw.Callbacks.errorCallbackPrint;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;

import org.lwjgl.Sys;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWvidmode;
import org.lwjgl.opengl.GLContext;

import com.Akoot.foxgame.config.GameSettings;
import com.Akoot.foxgame.entity.EntityMob;
import com.Akoot.foxgame.event.EventHandler;
import com.Akoot.foxgame.event.events.RenderEvent;
import com.Akoot.foxgame.event.events.TickEvent;
import com.Akoot.foxgame.graphics.Camera;
import com.Akoot.foxgame.gui.Gui;
import com.Akoot.foxgame.gui.GuiIngame;
import com.Akoot.foxgame.gui.Stage;
import com.Akoot.foxgame.input.KeyboardHandler;
import com.Akoot.foxgame.input.MouseHandler;
import com.Akoot.foxgame.level.Level;
import com.Akoot.foxgame.util.ResourceLocation;
import com.Akoot.foxgame.util.SharedLibraryLoader;
import com.Akoot.foxgame.util.Texture;

public class Foxgame
{
	/** Reference callback instances. */
	private GLFWErrorCallback errorCallback;
	private GLFWKeyCallback keyCallback;
	/* The window */
	public long window;

	/* Basic information */
	public static final String name = "Foxgame", version = "0.0.0", fullname = name + "-" + version;

	public final int initHeight = 830, initWidth = 1360;

	private static Foxgame game;
	public User user;
	public Gui currentScreen;
	public static EventHandler eventHandler = new EventHandler();
	public Level currentLevel;
	public static Camera camera;
	public static Stage stage;
	public GameSettings settings;

	private RenderEvent renderEvent;
	private TickEvent tickEvent;

	/** Run */
	public void run()
	{
		/* Print message */
		System.out.println("LWJGL " + Sys.getVersion() + " is working");
		renderEvent = new RenderEvent();
		tickEvent = new TickEvent();
		try
		{
			init();
			loop();

			/* Release window and window callbacks */
			glfwDestroyWindow(window);
			keyCallback.release();
		}
		finally
		{
			/* Terminate GLFW and release the GLFWerrorfun */
			glfwTerminate();
			errorCallback.release();
		}
	}

	/** Init */
	private void init()
	{

		/* Setup an error callback. The default implementation will print the error message in System.err. */
		glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));

		/* Initialize GLFW. Most GLFW functions will not work before doing this. */
		if ( glfwInit() != GL_TRUE )
			throw new IllegalStateException("Unable to initialize GLFW");

		/* Configure the window */
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

		/* Create the window */
		window = glfwCreateWindow(initWidth, initHeight, fullname, NULL, NULL);
		if (window == NULL) throw new RuntimeException("Failed to create the GLFW window.");

		/* Setup a key callback. It will be called every time a key is pressed, repeated or released. */
		glfwSetKeyCallback(window, keyCallback = new KeyboardHandler());
		glfwSetCursorPosCallback(window, new MouseHandler());

		/* Get primary monitor resolution */
		ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

		/* Center of the window */
		glfwSetWindowPos(window, (GLFWvidmode.width(vidmode) - initWidth) / 2, (GLFWvidmode.height(vidmode) - initHeight) / 2);

		/* Make the openGL context current */
		glfwMakeContextCurrent(window);

		/* Enable V-Sync */
		glfwSwapInterval(1);

		/* Display the window */
		glfwShowWindow(window);
	}

	public static Foxgame getFoxgame()
	{
		return game;
	}

	public void setLevel(Level level)
	{
		this.currentLevel = level;
	}

	public void setGuiScreen(Gui screen)
	{
		this.currentScreen = screen;
	}

	/** Main game loop */
	public void loop()
	{
		GLContext.createFromCurrent();

		/** Everything must be initiated AFTER this line */
		settings = new GameSettings();

		/** Bottom layer */
		stage = new Stage(this);

		/** Mid layer */
		EntityMob ghost = new EntityMob(this, "Ghost")
		{
			@Override
			public void render()
			{
				stage.drawTexture(x, y, 100, 100, texture);
			}
		};
		ghost.texture = new Texture(new ResourceLocation("assets/textures/player.png"));
		ghost.x = (float) Math.random() * 100;
		ghost.y = (float) Math.random() * 100;


		user = new User(this, "Jake111");

		/** Top layer */
		currentScreen = new GuiIngame(this);
		camera = new Camera(this);
		camera.setTarget(user.player);

		/** glState anything here */
		MouseHandler.showCursor(false);

		/* Set the background color*/
		glClearColor(1,1,1,1);

		/** Setup loop */

		/* Get nanoseconds */
		long lastTime = System.nanoTime();

		/* Nanoseconds per tick */
		double nsPerTick = 1000000000D / 60D; //60 ticks per second
		/* Nanoseconds per frame */
		double nsPerFrame = 1000000000D / settings.maxFPS; //max frames per second

		/* Get milliseconds */
		long lastTimer = System.currentTimeMillis();
		double deltaTicks = 0;
		double deltaFrames = 0;

		int frames = 0;
		int ticks = 0;

		/* Main game loop */
		while (glfwWindowShouldClose(window) == GL_FALSE)
		{
			/* Get current nanoseconds */
			long now = System.nanoTime();

			/* Increase deltas */
			deltaTicks += (now - lastTime) / nsPerTick;
			deltaFrames += (now - lastTime) / nsPerFrame;

			/* Reset time */
			lastTime = now;

			/* Don't render yet */
			boolean shouldRender = false;

			/* Should the game tick */
			while (deltaTicks >= 1)
			{
				tick();
				ticks++;
				deltaTicks -= 1;
			}

			/* Should the game render */
			while(deltaFrames >= 1)
			{
				if(!settings.capFrames)
				{
					/* Clear frame buffer */
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);	
					render();
					frames++;

					/* Swap color buffers */
					glfwSwapBuffers(window);

					/* Poll for window events */
					glfwPollEvents();
				}
				deltaFrames -= 1;
				shouldRender = true;
			}

			/* Render ONLY if it should */
			if (settings.capFrames && shouldRender)
			{
				/* Clear frame buffer */
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);	
				render();
				frames++;

				/* Swap color buffers */
				glfwSwapBuffers(window);

				/* Poll for window events */
				glfwPollEvents();
			}

			/* Reset every second */
			if (System.currentTimeMillis() - lastTimer >= 1000)
			{
				lastTimer += 1000;
				System.out.println(frames + " FPS; " + ticks + " ticks");
				frames = 0;
				ticks = 0;
			}
		}
	}

	/** Render all of the game */
	public void render()
	{
		/* Render everything else in the game */
		eventHandler.dispatchEvent(renderEvent);
	}

	/** Tick! */
	public void tick()
	{
		/* Do stuff per tick */
		eventHandler.dispatchEvent(tickEvent);
	}

	/** Main method of the entire program */
	public static void main(String[] args)
	{
		/* Load native libraries */
		SharedLibraryLoader.load();

		/* Launch the game */
		game = new Foxgame();
		game.run();
	}
}
