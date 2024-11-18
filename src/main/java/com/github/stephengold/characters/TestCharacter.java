/*
 Copyright (c) 2024 Stephen Gold

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.characters;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.CenterQuad;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.ui.AcorusDemo;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Locators;
import jme3utilities.ui.Overlay;

/**
 * Test a character model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TestCharacter extends AcorusDemo {
    // *************************************************************************
    // constants and loggers

    /**
     * index of the status line that indicates the animation clip
     */
    final private static int clipStatusLine = 0;
    /**
     * index of the status line that indicates when animation is paused
     */
    final private static int pausedStatusLine = 1;
    /**
     * number of status line in the overlay
     */
    final private static int numStatusLines = 2;
    /**
     * message logger for this class
     */
    final static Logger logger
            = Logger.getLogger(TestCharacter.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestCharacter.class.getSimpleName();
    /**
     * action string to advance to the next AnimClip
     */
    final private static String asNextClip = "next clip";
    /**
     * action string to orbit the camera to its left
     */
    final private static String asOrbitLeft = "orbit left";
    /**
     * action string to orbit the camera to its right
     */
    final private static String asOrbitRight = "orbit right";
    /**
     * action string to return to the previous AnimClip
     */
    final private static String asPreviousClip = "previous clip";
    // *************************************************************************
    // fields

    /**
     * composer of the C-G model that's on display
     */
    private static AnimComposer composer;
    /**
     * status overlay, displayed in the upper-left corner of the GUI node
     */
    private static Overlay statusOverlay;
    /**
     * name of the character to load
     */
    private static String characterName = "Erika";
    /**
     * name of the clip that's playing
     */
    private static String clipName;
    /**
     * sorted array of AnimClip names
     */
    private static String[] nameArray;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestCharacter application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Heart.setLoggingLevels(Level.WARNING);
        TestCharacter application = new TestCharacter();

        String title = applicationName + " " + MyString.join(arguments);
        if (arguments.length > 0) {
            characterName = arguments[0];
        }

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setAudioRenderer(null);
        settings.setResizable(true);
        settings.setSamples(4); // anti-aliasing
        settings.setTitle(title); // Customize the window's title bar.
        application.setSettings(settings);
        /*
         * The AWT settings dialog interferes with LWJGL v3
         * on macOS and Raspbian, so don't show it!
         */
        application.setShowSettings(false);
        application.start();
    }
    // *************************************************************************
    // AcorusDemo methods

    /**
     * Initialize this application.
     */
    @Override
    public void acorusInit() {
        // Configure the locators:
        String zip = Heart.fixPath(characterName + ".zip");
        Locators.useFilesystem(zip);
        Locators.registerDefault(); // to locate J3MDs and such

        switch (characterName) {
            case "Erika":
                addCharacter("Models/Mixamo/Erika Archer/scene.j3o");
                break;
            case "Vanguard":
                addCharacter(
                        "Models/Mixamo/Vanguard By T. Choonyung/scene.j3o");
                break;
            default:
                throw new RuntimeException(
                        "characterName = " + MyString.quote(characterName));
        }

        // Set the background to light blue.
        ColorRGBA backgroundColor = new ColorRGBA(0.2f, 0.2f, 1f, 1f);
        viewPort.setBackgroundColor(backgroundColor);

        // Attach world axes to the root node.
        float axesLength = 5f;
        attachWorldAxes(axesLength);

        generateMaterials(); // generate the floor material
        addFloor();

        addLighting();
        addStatusOverlay();
        configureCamera();
        //togglePause(); // to start paused
        getHelpBuilder().setBackgroundColor(ColorRGBA.Blue);

        super.acorusInit();
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();
        dim.bind(asNextClip,
                KeyInput.KEY_N, KeyInput.KEY_EQUALS, KeyInput.KEY_NUMPAD6);
        dim.bindSignal(asOrbitLeft, KeyInput.KEY_LEFT);
        dim.bindSignal(asOrbitRight, KeyInput.KEY_RIGHT);
        dim.bind(asPreviousClip,
                KeyInput.KEY_MINUS, KeyInput.KEY_NUMPAD4);
        dim.bind(asToggleHelp, KeyInput.KEY_H);
        dim.bind(asTogglePause, KeyInput.KEY_PAUSE, KeyInput.KEY_PERIOD);
        dim.bind(asToggleWorldAxes, KeyInput.KEY_SPACE);
    }

    /**
     * Process an action that wasn't handled by the active InputMode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case asNextClip:
                    // Select the next name in the array.
                    clipName = advanceString(nameArray, clipName, +1);
                    composer.setCurrentAction(clipName);
                    return;

                case asPreviousClip:
                    // Select the previous name in the array.
                    clipName = advanceString(nameArray, clipName, -1);
                    composer.setCurrentAction(clipName);
                    return;

                default:
            }
        }

        // The action has not been handled: forward it to the superclass.
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Update the GUI layout after the ViewPort gets resized.
     *
     * @param newWidth the new width of the ViewPort (in pixels, &gt;0)
     * @param newHeight the new height of the ViewPort (in pixels, &gt;0)
     */
    @Override
    public void onViewPortResize(int newWidth, int newHeight) {
        super.onViewPortResize(newWidth, newHeight);
        statusOverlay.onViewPortResize(newWidth, newHeight);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        // Update the status overlay.
        statusOverlay.setText(clipStatusLine, clipName);
        String pausedString = isPaused() ? "paused" : "";
        statusOverlay.setText(pausedStatusLine, pausedString);
    }
    // *************************************************************************
    // private methods

    /**
     * Attach an character model to the root node.
     */
    private void addCharacter(String assetPath) {
        Node modelRoot = (Node) assetManager.loadModel(assetPath);
        rootNode.attachChild(modelRoot);

        setCgmHeight(modelRoot, 20f); // Make it 20 world units tall.
        centerCgm(modelRoot);         // Position it above the origin.

        // Find the AnimComposer:
        List<AnimComposer> composers
                = MySpatial.listControls(modelRoot, AnimComposer.class, null);
        assert composers.size() == 1;
        composer = composers.get(0);

        nameArray = composer.getAnimClipsNames().toArray(new String[0]);
        Arrays.sort(nameArray);
        clipName = "( no clip loaded )";

        // Enumerate all animation clips to the console:
        for (String name : nameArray) {
            System.out.println(name);
        }
        printSummary(modelRoot, composer);
    }

    /**
     * Add a horizontal green square the root node.
     */
    private void addFloor() {
        float width = 100f;
        float height = 100f;
        Mesh squareMesh = new CenterQuad(width, height);
        Geometry floor = new Geometry("floor", squareMesh);
        floor.rotate(-FastMath.HALF_PI, 0f, 0f);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);

        // Retrieve an appropriate material from the library.
        Material material = findMaterial("platform");

        floor.setMaterial(material);
        rootNode.attachChild(floor);
    }

    /**
     * Add lighting and shadows to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -2f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.5f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Add a status overlay to the GUI scene.
     */
    private void addStatusOverlay() {
        float width = 275f; // in pixels
        statusOverlay = new Overlay("status", width, numStatusLines);

        boolean success = stateManager.attach(statusOverlay);
        assert success;

        statusOverlay.setEnabled(true);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(9f);

        cam.setLocation(new Vector3f(0f, 30f, 40f));
        cam.setRotation(new Quaternion(0f, 0.96300f, -0.26915f, 0f));

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, asOrbitLeft, asOrbitRight);
        stateManager.attach(orbitState);
    }

    /**
     * Summarize of the specified model asset to {@code System.out}.
     *
     * @param modelRoot the model's root spatial (not null, unaffected)
     * @param composer the model's AnimComposer (not null, unaffected)
     */
    private static void printSummary(
            Spatial modelRoot, AnimComposer composer) {
        List<Mesh> meshList = MyMesh.listMeshes(modelRoot, null);
        int numMeshes = meshList.size();

        Collection<AnimClip> clips = composer.getAnimClips();
        int numClips = clips.size();

        int numVertices = MySpatial.countVertices(modelRoot);

        System.err.flush();
        System.out.printf("%nJ3O model asset with %d mesh%s, "
                + "%d animation clip%s, and %d vert%s%n",
                numMeshes, (numMeshes == 1) ? "" : "es",
                numClips, (numClips == 1) ? "" : "s",
                numVertices, (numVertices == 1) ? "ex" : "ices");
    }
}
