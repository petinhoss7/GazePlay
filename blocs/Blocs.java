package blocs;

/**
 * Created by schwab on 29/10/2016.
 */

import com.sun.glass.ui.Screen;
import gaze.GazeEvent;
import gaze.GazeUtils;
import gaze.SecondScreen;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import utils.games.Bravo;
import utils.games.Utils;
import utils.games.stats.HiddenItemsGamesStats;

import java.util.ArrayList;

public class Blocs extends Application {

    private static EventHandler<Event> enterEvent;
    private static Group blockRoot;
    private static int count;
    private static int initCount;
    private static float p4w;
    private static boolean finished = false;
    private static Scene theScene;
    private static int nColomns;
    private static int nLines;
    private static boolean hasColors;
    private static Bravo bravo = new Bravo();
    private static ChoiceBox<String> choiceBox;
    private static ArrayList<ArrayList<Bloc>> blocs;
    private static final int trail = 10;
    private static final Image[] images = Utils.images(System.getProperty("user.home") +Utils.FILESEPARATOR+ "GazePlay"+Utils.FILESEPARATOR+"files"+Utils.FILESEPARATOR+"images"+Utils.FILESEPARATOR+"blocs"+Utils.FILESEPARATOR);

    public static void main(String[] args) {Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Blocs");

        primaryStage.setFullScreen(true);

        blockRoot = new Group();

        theScene = new Scene(blockRoot, Screen.getScreens().get(0).getWidth(), Screen.getScreens().get(0).getHeight(), Color.BLACK);

        primaryStage.setOnCloseRequest((WindowEvent we) -> System.exit(0));

        primaryStage.setScene(theScene);

        HiddenItemsGamesStats stats = new HiddenItemsGamesStats(theScene);

        makeBlocks(theScene, blockRoot, null, 2, 2, true, 1, false, stats);

        primaryStage.show();

        SecondScreen secondScreen = SecondScreen.launch();
    }

    public static void makeBlocks(Scene scene, Group root, ChoiceBox<String> cbxGames, int nbLines, int nbColomns, boolean colors, float percents4Win, boolean useTrail, HiddenItemsGamesStats stats){

        finished = false;

        p4w = percents4Win;

        blockRoot=root;

        nColomns = nbColomns;

        nLines = nbLines;

        hasColors = colors;

        theScene = scene;

        choiceBox = cbxGames;

        blockRoot.getChildren().add(bravo);

        blocs = new ArrayList<>(nbColomns);
        for(int i = 0; i < nbLines; i++)
            blocs.add(new ArrayList<>(nbLines));

        int value = (int)Math.floor(Math.random()*images.length);

        scene.setFill(new ImagePattern(images[value]));

        enterEvent = buildEvent(stats, useTrail);

        int width = (int)(scene.getWidth() / nbColomns);
        int height = (int)(scene.getHeight() / nbLines);

        initCount = nbColomns * nbLines;

        count = initCount;

        for (int i = 0; i < nbColomns; i++)
            for (int j = 0; j < nbLines; j++) {

                Bloc bloc = new Bloc(i * width, j * height, width, height,i ,j);
                if(colors)
                    bloc.setFill(new Color(Math.random(), Math.random(), Math.random(), 1));
                else
                    bloc.setFill(Color.BLACK);
                root.getChildren().add(bloc);
                blocs.get(i).add(bloc);

                bloc.toBack();

                GazeUtils.addEventFilter(bloc);

                bloc.addEventFilter(MouseEvent.ANY, enterEvent);

                bloc.addEventFilter(GazeEvent.ANY, enterEvent);

                stats.start();
            }
    }

    private static void removeBloc(Bloc toRemove){

        toRemove.removeEventFilter(MouseEvent.ANY, enterEvent);
        toRemove.removeEventFilter(GazeEvent.ANY, enterEvent);
        GazeUtils.removeEventFilter(toRemove);
        toRemove.setTranslateX(-10000);
        toRemove.setOpacity(0);
        count--;
    }

    private static EventHandler<Event> buildEvent(HiddenItemsGamesStats stats, boolean useTrail) {
        return new EventHandler<Event>() {
            @Override
            public void handle(Event e) {

                if(!finished && e.getEventType().equals(MouseEvent.MOUSE_ENTERED) || e.getEventType().equals(GazeEvent.GAZE_ENTERED)) {

                    if(! useTrail) {
                        Bloc bloc = (Bloc) e.getTarget();

                    bloc.removeEventFilter(MouseEvent.ANY, enterEvent);
                    bloc.removeEventFilter(GazeEvent.ANY, enterEvent);
                    GazeUtils.removeEventFilter(bloc);
                    bloc.setTranslateX(-10000);
                    bloc.setOpacity(0);
                    count--;

                    }
                    else {

                        Bloc bloc = (Bloc) e.getTarget();

                        int posX = bloc.posX;
                        int posY = bloc.posY;

                        for (int i = -trail; i < trail; i++)
                            for (int j = -trail; j < trail; j++) {

                                if (Math.sqrt(i * i + j * j) < trail && posX + i >= 0 && posY + j >= 0 && posX + i < blocs.size() && posY + j < blocs.get(0).size())
                                    removeBloc(blocs.get(posX + i).get(posY + j));
                            }
                    }
                    if(((float)initCount-count)/initCount >= p4w && !finished){

                        finished = true;

                        stats.incNbGoals();

                        for(Node N : blockRoot.getChildren()){

                            //if(! (N instanceof Home) && ! (N instanceof Bravo)) {

                            if(! (N instanceof Bravo)) {

                                N.setTranslateX(-10000);
                                N.setOpacity(0);
                                N.removeEventFilter(MouseEvent.ANY, enterEvent);

                                //R.removeEventFilter(GazeEvent.ANY, enterEvent);
                                //GazeUtils.removeEventFilter(R);
                            }
                        }

                        SequentialTransition sequence = bravo.win();
                        sequence.setOnFinished(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent actionEvent) {
                                Utils.clear(theScene, blockRoot, choiceBox);
                                makeBlocks(theScene, blockRoot, choiceBox, nLines, nColomns, hasColors, p4w, useTrail, stats);
                                Utils.home(theScene, blockRoot, choiceBox, stats);
                            }
                        });
                    }


                }
            }
        };
    }
}

class Bloc extends Rectangle{

    public int posX;
    public int posY;


    public Bloc(double x, double y, double width, double height, int posX, int posY) {
        super(x, y, width, height);
        this.posX=posX;
        this.posY=posY;
    }
}
