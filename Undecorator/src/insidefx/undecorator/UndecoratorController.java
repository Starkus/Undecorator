/*
 * Copyright 2014-2016 Arnaud Nouard. All rights reserved.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package insidefx.undecorator;

import java.util.logging.Level;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 *
 * @author in-sideFX
 */
public class UndecoratorController {
	
	Dock lastDocked = Dock.NONE;
	private static double initXStage = -1;
	private static double initYStage = -1;
	private static double initXCursor = -1;
	private static double initYCursor = -1;
	private static double newX;
	private static double newY;
	private static int RESIZE_PADDING;
	private static int SHADOW_WIDTH;
	Undecorator undecorator;
	BoundingBox savedBounds, savedFullScreenBounds;
	boolean maximized = false;
	static boolean isMacOS = false;
	static final int MAXIMIZE_BORDER = 20;  // Allow double click to maximize on top of the Scene

	{
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("mac") != -1) {
			isMacOS = true;
		}
	}

	public UndecoratorController(Undecorator ud) {
		undecorator = ud;
	}


	/*
	 * Actions
	 */
	protected void maximizeOrRestore() {

		Stage stage = undecorator.getStage();

		if (maximized) {
			restoreSavedBounds(stage, false);
			undecorator.setShadow(true);
			savedBounds = null;
			maximized = false;
		} else {
			ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
			Screen screen = screensForRectangle.get(0);
			Rectangle2D visualBounds = screen.getVisualBounds();

			savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

			undecorator.setShadow(false);

			stage.setX(visualBounds.getMinX());
			stage.setY(visualBounds.getMinY());
			stage.setWidth(visualBounds.getWidth());
			stage.setHeight(visualBounds.getHeight());
			maximized = true;
		}
	}

	public void saveBounds() {
		Stage stage = undecorator.getStage();
		savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
	}

	public void saveFullScreenBounds() {
		Stage stage = undecorator.getStage();
		savedFullScreenBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
	}

	public void restoreSavedBounds(Stage stage, boolean fullscreen) {

		stage.setX(savedBounds.getMinX());
		stage.setY(savedBounds.getMinY());
		stage.setWidth(savedBounds.getWidth());
		stage.setHeight(savedBounds.getHeight());
		savedBounds = null;
	}

	public void restoreFullScreenSavedBounds(Stage stage) {

		stage.setX(savedFullScreenBounds.getMinX());
		stage.setY(savedFullScreenBounds.getMinY());
		stage.setWidth(savedFullScreenBounds.getWidth());
		stage.setHeight(savedFullScreenBounds.getHeight());
		savedFullScreenBounds = null;
	}

	public void close() {
		final Stage stage = undecorator.getStage();
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
			}
		});

	}

	public void minimize() {

		if (!Platform.isFxApplicationThread()) // Ensure on correct thread else hangs X under Unbuntu
		{
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					_minimize();
				}
			});
		} else {
			_minimize();
		}
	}

	private void _minimize() {
		Stage stage = undecorator.getStage();
		stage.setIconified(true);
	}

	/**
	 * Stage resize management
	 *
	 * @param stage
	 * @param node
	 * @param PADDING
	 * @param SHADOW
	 */
	public void setStageResizableWith(final Stage stage, final Node node, int PADDING, int SHADOW) {

		RESIZE_PADDING = PADDING;
		SHADOW_WIDTH = SHADOW;
		node.setOnMouseClicked(new EventHandler<MouseEvent>() {
			// Maximize on double click
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && mouseEvent.getClickCount() > 1) {
					if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
						undecorator.maximizeProperty().set(!undecorator.maximizeProperty().get());
						mouseEvent.consume();
					}
				}
			}
		});

		node.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.isPrimaryButtonDown()) {
					initXCursor = mouseEvent.getScreenX();
					initYCursor = mouseEvent.getScreenY();
					mouseEvent.consume();
				}
			}
		});
		node.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (!mouseEvent.isPrimaryButtonDown() || (initXCursor == -1 && initYCursor == -1)) {
					return;
				}
				if (stage.isFullScreen()) {
					return;
				}
				/*
				 * Long press generates drag event!
				 */
				if (mouseEvent.isStillSincePress()) {
					return;
				}
				if (maximized) {
					// Remove maximized state
					undecorator.maximizeProperty.set(false);
					return;
				} // Docked then moved, so restore state
				else if (savedBounds != null) {
					undecorator.setShadow(true);
				}

				newX = mouseEvent.getScreenX();
				newY = mouseEvent.getScreenY();
				double deltax = newX - initXCursor;
				double deltay = newY - initYCursor;

				Cursor cursor = node.getCursor();
				if (Cursor.E_RESIZE.equals(cursor)) {
					setStageWidth(stage, stage.getWidth() + deltax);
					mouseEvent.consume();
				} else if (Cursor.NE_RESIZE.equals(cursor)) {
					if (setStageHeight(stage, stage.getHeight() - deltay)) {
						setStageY(stage, stage.getY() + deltay);
					}
					setStageWidth(stage, stage.getWidth() + deltax);
					mouseEvent.consume();
				} else if (Cursor.SE_RESIZE.equals(cursor)) {
					setStageWidth(stage, stage.getWidth() + deltax);
					setStageHeight(stage, stage.getHeight() + deltay);
					mouseEvent.consume();
				} else if (Cursor.S_RESIZE.equals(cursor)) {
					setStageHeight(stage, stage.getHeight() + deltay);
					mouseEvent.consume();
				} else if (Cursor.W_RESIZE.equals(cursor)) {
					if (setStageWidth(stage, stage.getWidth() - deltax)) {
						stage.setX(stage.getX() + deltax);
					}
					mouseEvent.consume();
				} else if (Cursor.SW_RESIZE.equals(cursor)) {
					if (setStageWidth(stage, stage.getWidth() - deltax)) {
						stage.setX(stage.getX() + deltax);
					}
					setStageHeight(stage, stage.getHeight() + deltay);
					mouseEvent.consume();
				} else if (Cursor.NW_RESIZE.equals(cursor)) {
					if (setStageWidth(stage, stage.getWidth() - deltax)) {
						stage.setX(stage.getX() + deltax);
					}
					if (setStageHeight(stage, stage.getHeight() - deltay)) {
						setStageY(stage, stage.getY() + deltay);
					}
					mouseEvent.consume();
				} else if (Cursor.N_RESIZE.equals(cursor)) {
					if (setStageHeight(stage, stage.getHeight() - deltay)) {
						setStageY(stage, stage.getY() + deltay);
					}
					mouseEvent.consume();
				}

			}
		});
		node.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (maximized) {
					setCursor(node, Cursor.DEFAULT);
					return; // maximized mode does not support resize
				}
				if (stage.isFullScreen()) {
					return;
				}
				if (!stage.isResizable()) {
					return;
				}
				double x = mouseEvent.getX();
				double y = mouseEvent.getY();
				Bounds boundsInParent = node.getBoundsInParent();
				if (isRightEdge(x, y, boundsInParent)) {
					if (y < RESIZE_PADDING + SHADOW_WIDTH) {
						setCursor(node, Cursor.NE_RESIZE);
					} else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
						setCursor(node, Cursor.SE_RESIZE);
					} else {
						setCursor(node, Cursor.E_RESIZE);
					}

				} else if (isLeftEdge(x, y, boundsInParent)) {
					if (y < RESIZE_PADDING + SHADOW_WIDTH) {
						setCursor(node, Cursor.NW_RESIZE);
					} else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
						setCursor(node, Cursor.SW_RESIZE);
					} else {
						setCursor(node, Cursor.W_RESIZE);
					}
				} else if (isTopEdge(x, y, boundsInParent)) {
					setCursor(node, Cursor.N_RESIZE);
				} else if (isBottomEdge(x, y, boundsInParent)) {
					setCursor(node, Cursor.S_RESIZE);
				} else {
					setCursor(node, Cursor.DEFAULT);
				}
			}
		});
	}

	/**
	 * Under Windows, the undecorator Stage could be been dragged below the Task bar and then no way to grab it again...
	 * On Mac, do not drag above the menu bar
	 *
	 * @param y
	 */
	void setStageY(Stage stage, double y) {
		try {
			ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
			for (Screen screen : screensForRectangle) {
				Rectangle2D visualBounds = screen.getVisualBounds();
				if (y < visualBounds.getHeight() - 30 && y + SHADOW_WIDTH >= visualBounds.getMinY()) {
					stage.setY(y);
					return;
				}
			}
		} catch (Exception e) {
			Undecorator.LOGGER.log(Level.SEVERE, "setStageY issue", e);
		}
	}

	boolean setStageWidth(Stage stage, double width) {
		if (width >= stage.getMinWidth()) {
			stage.setWidth(width);
			initXCursor = newX;
			return true;
		}
		return false;
	}

	boolean setStageHeight(Stage stage, double height) {
		if (height >= stage.getMinHeight()) {
			stage.setHeight(height);
			initYCursor = newY;
			return true;
		}
		return false;
	}

	/**
	 * Allow this node to drag the Stage
	 *
	 * @param stage
	 * @param node
	 */
	public void setAsStageDraggable(final Stage stage, final Node node) {

		node.setOnMouseClicked(new EventHandler<MouseEvent>() {
			// Maximize on double click
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && stage.isResizable() && mouseEvent.getClickCount() > 1) {
					if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
						undecorator.maximizeProperty().set(!undecorator.maximizeProperty().get());
						mouseEvent.consume();
					}
				}
			}
		});
		node.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.isPrimaryButtonDown()) {
					initXCursor = mouseEvent.getScreenX();
					initYCursor = mouseEvent.getScreenY();
					initXStage = stage.getX();
					initYStage = stage.getY();
					mouseEvent.consume();
				} else {
					initXCursor = -1;
					initYCursor = -1;
				}
			}
		});
		node.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (!mouseEvent.isPrimaryButtonDown() || initXCursor == -1) {
					return;
				}
				if (stage.isFullScreen()) {
					return;
				}
				/*
				 * Long press generates drag event!
				 */
				if (mouseEvent.isStillSincePress()) {
					return;
				}
				if (maximized) {
					// Remove Maximized state
					undecorator.maximizeProperty.set(false);
					// Center 
					stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
					stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
				} // Docked then moved, so restore state
				else if (savedBounds != null) {
					restoreSavedBounds(stage, false);
					undecorator.setShadow(true);
					// Center
					stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
					stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
				}
				double newX = mouseEvent.getScreenX();
				double newY = mouseEvent.getScreenY();
				double deltax = newX - initXCursor;
				double deltay = newY - initYCursor;
				//initX = newX;
				//initY = newY;
				setCursor(node, Cursor.DEFAULT);
				stage.setX(initXStage + deltax);
				setStageY(stage, initYStage + deltay);

				testDock(stage, mouseEvent);
				mouseEvent.consume();

				///////////////////////
				//                Robot robot = null;
				//                try {
				//                    robot = new Robot();
				//                } catch (AWTException ex) {
				//                }
				//                stage.getScene().getRoot().setVisible(false);
				//                BufferedImage screenShot = robot.createScreenCapture(new Rectangle((int) stage.getX(), (int) stage.getY(), (int) stage.getWidth(), (int) 40));
				//                stage.getScene().getRoot().setVisible(true);
				//                Image background = SwingFXUtils.toFXImage(screenShot, null);
				//
				//                ImagePattern imagePattern = new ImagePattern(background);
				////                undecorator.getBackgroundRectangle().setEffect(new BoxBlur());
				//                undecorator.getBackgroundRectangle().setFill(imagePattern);
				////////////////
			}
		});
		node.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent t) {
				if (stage.isResizable()) {
					undecorator.setDockFeedbackInvisible();
					setCursor(node, Cursor.DEFAULT);
					initXCursor = -1;
					initYCursor = -1;
					dockActions(stage, t);
				}
			}
		});

		node.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				//setCursor(node, Cursor.DEFAULT);
			}
		});

	}

	private Rectangle2D mouseRect(MouseEvent e) {
		final int radius = 3;

		return new Rectangle2D(e.getScreenX() - radius, 
				e.getScreenY() - radius,
				radius * 2,
				radius * 2);
	}

	/**
	 * (Humble) Simulation of Windows behavior on screen's edges Feedbacks
	 */
	void testDock(Stage stage, MouseEvent mouseEvent) {

		if (!stage.isResizable()) {
			return;
		}

		Rectangle2D rect = mouseRect(mouseEvent);
		ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(rect);

		if (screensForRectangle.isEmpty())
			return;

		Screen screen = screensForRectangle.get(0);
		Rectangle2D visualBounds = screen.getVisualBounds();

		Dock dockSide = getDockSide(mouseEvent);
		// Dock Left
		if (dockSide == Dock.LEFT) {
			if (lastDocked == Dock.LEFT) {
				return;
			}
			// Dock Left
			double x = visualBounds.getMinX();
			double y = visualBounds.getMinY();
			double width = visualBounds.getWidth() / 2;
			double height = visualBounds.getHeight();

			undecorator.setDockFeedbackVisible(mouseEvent, x, y, width, height);
			lastDocked = Dock.LEFT;
		} // Dock Right
		else if (dockSide == Dock.RIGHT) {
			if (lastDocked == Dock.RIGHT) {
				return;
			}
			// Dock Right (visualBounds = (javafx.geometry.Rectangle2D) Rectangle2D [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
			double x = visualBounds.getMinX() + visualBounds.getWidth() / 2;
			double y = visualBounds.getMinY();
			double width = visualBounds.getWidth() / 2;
			double height = visualBounds.getHeight();

			undecorator.setDockFeedbackVisible(mouseEvent, x, y, width, height);
			lastDocked = Dock.RIGHT;
		} // Dock top
		else if (dockSide == Dock.TOP) {
			if (lastDocked == Dock.TOP) {
				return;
			}
			// Dock Left
			double x = visualBounds.getMinX();
			double y = visualBounds.getMinY();
			double width = visualBounds.getWidth();
			double height = visualBounds.getHeight();
			undecorator.setDockFeedbackVisible(mouseEvent, x, y, width, height);
			lastDocked = Dock.TOP;
		} else {
			undecorator.setDockFeedbackInvisible();
			lastDocked = Dock.NONE;
		}
	}

	/**
	 * Based on mouse position returns dock side
	 *
	 * @param mouseEvent
	 * @return DOCK_LEFT,DOCK_RIGHT,DOCK_TOP
	 */
	Dock getDockSide(MouseEvent mouseEvent) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = 0;
		double maxY = 0;

		Rectangle2D rect = mouseRect(mouseEvent);
		ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(rect);

		for (Screen screen : screensForRectangle) {
			Rectangle2D visualBounds = screen.getVisualBounds();
			minX = Math.min(minX, visualBounds.getMinX());
			minY = Math.min(minY, visualBounds.getMinY());
			maxX = Math.max(maxX, visualBounds.getMaxX());
			maxY = Math.max(maxY, visualBounds.getMaxY());
		}
		// Dock Left
		if (mouseEvent.getScreenX() == minX) {
			return Dock.LEFT;
		} else if (mouseEvent.getScreenX() >= maxX - 1) { // MaxX returns the width? Not width -1 ?!
			return Dock.RIGHT;
		} else if (mouseEvent.getScreenY() <= minY) {   // Mac menu bar
			return Dock.TOP;
		}
		return Dock.NONE;
	}

	/**
	 * (Humble) Simulation of Windows behavior on screen's edges Actions
	 */
	void dockActions(Stage stage, MouseEvent mouseEvent) {

		Rectangle2D rect = mouseRect(mouseEvent);
		ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(rect);

		Screen screen = screensForRectangle.get(0);
		Rectangle2D visualBounds = screen.getVisualBounds();
		// Dock Left
		if (mouseEvent.getScreenX() == visualBounds.getMinX()) {
			savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

			stage.setX(visualBounds.getMinX());
			stage.setY(visualBounds.getMinY());
			// Respect Stage Max size
			double width = visualBounds.getWidth() / 2;
			if (stage.getMaxWidth() < width) {
				width = stage.getMaxWidth();
			}

			stage.setWidth(width);

			double height = visualBounds.getHeight();
			if (stage.getMaxHeight() < height) {
				height = stage.getMaxHeight();
			}

			stage.setHeight(height);
			undecorator.setShadow(false);
		} // Dock Right (visualBounds = [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
		else if (mouseEvent.getScreenX() >= visualBounds.getMaxX() - 1) { // MaxX returns the width? Not width -1 ?!
			savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

			stage.setX(visualBounds.getWidth() / 2 + visualBounds.getMinX());
			stage.setY(visualBounds.getMinY());
			// Respect Stage Max size
			double width = visualBounds.getWidth() / 2;
			if (stage.getMaxWidth() < width) {
				width = stage.getMaxWidth();
			}

			stage.setWidth(width);

			double height = visualBounds.getHeight();
			if (stage.getMaxHeight() < height) {
				height = stage.getMaxHeight();
			}

			stage.setHeight(height);
			undecorator.setShadow(false);
		} else if (mouseEvent.getScreenY() <= visualBounds.getMinY()) { // Mac menu bar
			undecorator.maximizeProperty.set(true);
		}

	}

	public boolean isRightEdge(double x, double y, Bounds boundsInParent) {
		if (x < boundsInParent.getWidth() && x > boundsInParent.getWidth() - RESIZE_PADDING) {
			return true;
		}
		return false;
	}

	public boolean isTopEdge(double x, double y, Bounds boundsInParent) {
		if (y >= 0 && y < RESIZE_PADDING) {
			return true;
		}
		return false;
	}

	public boolean isBottomEdge(double x, double y, Bounds boundsInParent) {
		if (y < boundsInParent.getHeight() && y > boundsInParent.getHeight() - RESIZE_PADDING) {
			return true;
		}
		return false;
	}

	public boolean isLeftEdge(double x, double y, Bounds boundsInParent) {
		if (x >= 0 && x < RESIZE_PADDING) {
			return true;
		}
		return false;
	}

	public void setCursor(Node n, Cursor c) {
		n.setCursor(c);
	}


	private enum Dock {
		NONE, LEFT, RIGHT, TOP;
	}
}
