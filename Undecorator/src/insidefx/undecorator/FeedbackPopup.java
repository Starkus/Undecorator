package insidefx.undecorator;

import javafx.animation.FadeTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class FeedbackPopup extends Stage {

	private final int shadowSize = 20;

	FadeTransition fadeTransition;


	public FeedbackPopup()
	{
		super(StageStyle.TRANSPARENT);

		build();
		
		show();
		hide();
	}


	public void show(MouseEvent e, Rectangle2D rect)
	{
		fadeTransition.setOnFinished(null);
		
		setX(rect.getMinX());
		setY(rect.getMinY());
		setWidth(rect.getWidth());
		setHeight(rect.getHeight());

		fadeTransition.stop();
		fadeTransition.setFromValue(0);
		fadeTransition.setToValue(1);
		fadeTransition.play();

		show();
	}
	
	@Override
	public void hide() {

		System.out.println(getOpacity());
		
		fadeTransition.setFromValue(getOpacity());
		fadeTransition.setToValue(0);
		fadeTransition.play();
			
		fadeTransition.setOnFinished(e -> super.hide());
	}


	/**
	 * Prepare Stage for dock feedback display
	 */
	private void build()
	{
		StackPane pane = new StackPane(createShadowPane(), createBorderPane());
		pane.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);"
					+ "-fx-background-insets: " + shadowSize + ";"
		);
		
		pane.setCache(true);
		pane.setCacheHint(CacheHint.SPEED);
		pane.setMouseTransparent(true);

		Scene scene = new Scene(pane);
		scene.setFill(Color.TRANSPARENT);
		setScene(scene);
		//sizeToScene();
		
		fadeTransition = new FadeTransition(Duration.seconds(0.2));
		fadeTransition.setNode(pane);
	}


	private Pane createShadowPane()
	{
		Pane shadowPane = new Pane();

		shadowPane.setStyle(
				  "-fx-background-color: white;"
				+ "-fx-effect: dropshadow(gaussian, black, " + shadowSize + ", 0, 0, 0);"
				+ "-fx-background-insets: " + shadowSize + ";"
		);

		Rectangle innerRect = new Rectangle();
		Rectangle outerRect = new Rectangle();

		shadowPane.layoutBoundsProperty().addListener((obs, oldv, newv) ->
		{
			innerRect.relocate(newv.getMinX() + shadowSize, newv.getMinY() + shadowSize);
			innerRect.setWidth(newv.getWidth() - shadowSize * 2);
			innerRect.setHeight(newv.getHeight() - shadowSize * 2);

			outerRect.relocate(newv.getMinX(), newv.getMinY());
			outerRect.setWidth(newv.getWidth());
			outerRect.setHeight(newv.getHeight());

			Shape clip = Shape.subtract(outerRect, innerRect);
			shadowPane.setClip(clip);
		});

		return shadowPane;
	}

	private Pane createBorderPane()
	{
		Pane borderPane = new Pane();

		borderPane.setStyle(
				  "-fx-background-color: null;"
				+ "-fx-border-color: rgba(255, 255, 255, 0.25);"
				+ "-fx-border-insets: " + shadowSize + ";"
		);

		return borderPane;
	}
}
