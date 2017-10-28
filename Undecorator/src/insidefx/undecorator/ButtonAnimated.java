package insidefx.undecorator;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ButtonAnimated extends Button {

	private final ObjectProperty<Color> backgroundNormalColor = new SimpleObjectProperty<>(new Color(0, 0, 0, 0));
	private final ObjectProperty<Color> backgroundHoverColor = new SimpleObjectProperty<>(new Color(1, 0, 0, 1));
	
	private Duration fadeDuration = new Duration(100);
	
	private Timeline hoverOn, hoverOff;
	
	private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(backgroundNormalColor.get());
	private final StringProperty backgroundColorString = createColorStringProperty(backgroundColor);
	
	
	public ButtonAnimated()
	{
		super();
		
		updateTimelines();
		bindAnimations();
		
		backgroundNormalColor.addListener((obs, oldv, newv) -> {
			updateTimelines();
		});
		backgroundHoverColor.addListener((obs, oldv, newv) -> {
			updateTimelines();
		});
		
		hoverProperty().addListener((obs, oldv, newv) -> {
			if (newv)
				hoverOn.play();
			else
				hoverOff.play();
		});
	}
	
	private void updateTimelines()
	{
		hoverOn = new Timeline(
				new KeyFrame(Duration.seconds(0),	new KeyValue(backgroundColor, backgroundNormalColor.get())),
				new KeyFrame(fadeDuration,			new KeyValue(backgroundColor, backgroundHoverColor.get()))
				);

		hoverOff = new Timeline(
				new KeyFrame(Duration.seconds(0),	new KeyValue(backgroundColor, backgroundHoverColor.get())),
				new KeyFrame(fadeDuration,			new KeyValue(backgroundColor, backgroundNormalColor.get()))
				);
	}
	
	private void bindAnimations()
	{
		styleProperty().bind(
				new SimpleStringProperty("-fx-background-color: ")
				.concat(backgroundColorString)
				.concat(";")
		);
	}
	
	private StringProperty createColorStringProperty(ObjectProperty<Color> observableColor) {
		
		final StringProperty prop = new SimpleStringProperty();
		setColorStringFromColor(prop, observableColor);
		
		observableColor.addListener(new ChangeListener<Color>() {

			@Override
			public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
				setColorStringFromColor(prop, observableColor);
			}
		});
		
		return prop;
	}
	
	private void setColorStringFromColor(StringProperty colorStringProperty, ObjectProperty<Color> color) {
        colorStringProperty.set(
                "rgba("
                        + ((int) (color.get().getRed()   * 255)) + ","
                        + ((int) (color.get().getGreen() * 255)) + ","
                        + ((int) (color.get().getBlue()  * 255)) + ","
                        + String.format(java.util.Locale.US, "%.4f", color.get().getOpacity()) +
                ")"
        );
    }
	
	
	public Color getBackgroundNormalColor() {
		return backgroundNormalColor.get();
	}
	public void setBackgroundNormalColor(Color backgroundNormalColor) {
		this.backgroundNormalColor.set(backgroundNormalColor);
	}
	public ObjectProperty<Color> backgroundNormalColorProperty()
	{
		return backgroundNormalColor;
	}
	
	public Color getBackgroundHoverColor() {
		return backgroundHoverColor.get();
	}
	public void setBackgroundHoverColor(Color backgroundHoverColor) {
		this.backgroundHoverColor.set(backgroundHoverColor);
	}
	public ObjectProperty<Color> backgroundHoverColorProperty()
	{
		return backgroundHoverColor;
	}
	
	public Duration getFadeDuration() {
		return fadeDuration;
	}
	public void setFadeDuration(Duration fadeDuration) {
		this.fadeDuration = fadeDuration;
	}
}
