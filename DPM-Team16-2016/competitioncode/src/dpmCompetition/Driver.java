package dpmCompetition;

/**
 * Provides robot the capabilities of traveling to different destinations
 * 
 */
public class Driver {
	
	/** Reference to the odometer thread used by the robot */ 
	private Odometer odometer;
	/** Reference to the navigation used by the robot */
	private Navigation navigation;
	/** Reference to the Competition's data */
	private CompetitionData competitionData;
	/** Reference to the Scanner */
	private AreaScanner areaScanner;
	/** Reference to the lsPoller */
	private LsPoller lsPoller;
	
	/** The acceptable distance for the robot to determine if it is close enough to the target */
	private static final double acceptableError = 2;
	/** The time interval before continuing the next loop */
	private final int TIMEOUT_PERIOD = 20;
    /** The offset for the robot to travel to the center of a square  */
	private final double squareOffset = 20.0;

	
	/**
	 * Constructor 
	 * 
	 * @param odometer a reference to the odometer thread used by the robot.
	 * @param navigation a reference to the navigation use by the robot
	 * @param competitionData a reference to the competition's data
	 * @param areaScanner a reference to the scanner
	 */
	Driver(Odometer odometer, Navigation navigation, CompetitionData competitionData, AreaScanner areaScanner, LsPoller lsPoller) {

		this.odometer = odometer;
		this.navigation = navigation;
		this.competitionData = competitionData;
		this.areaScanner = areaScanner;
		this.lsPoller = lsPoller;

	}

	/**
	 * The robot travels to every block that it finds
	 */
	public void travelToBlueBlock() {
		Block[] blocks = areaScanner.findCloseObjects();
		
		double initialX = odometer.getX();
		double initialY = odometer.getY();
		for (int i=0; i < 5; i++) {
			for (Block block: blocks) {
				Display.print(block.center.x, 4);
				Display.print(block.center.y, 5);
				Coordinate coord = removeOffset(block.center, Main.US_OFFSET);
				
				travelTo(coord.x, coord.y);
				Helper.sleep(200);
				if (isBlueBlock()) {
					return;
				} else /*if (isObstacle())*/{
					navigation.goForward(-10);
					travelTo(initialX, initialY);
				}
			}
			initialX += 20;
			initialY += 20;
			travelTo(initialX, initialY);
			navigation.turnTo(0);
		}
		
	}
	/**
	 * 
	 * @return if the robot has seen a blue block
	 */
	private boolean isBlueBlock() {
		return lsPoller.isSeeingBlueBlock();
		
	}
	
	/**
	 * 
	 * @return if the robot has seen an obstacle
	 */
	private boolean isObstacle() {
		
		return lsPoller.isSeeingWoodenBlock();
		
	}
	
	/**
	 * Remove the offset cause by the distance between the sensors and the wheels.
	 * 
	 * @param coord position of the object
	 * @param offset the horizontal distance between the sensors and the wheels
	 * @return the new position of the object after the offset has been removed
	 */
	private Coordinate removeOffset(Coordinate coord, double offset) {
		
		double angle = Math.atan2(coord.y, coord.x);
		Coordinate newCoord = new Coordinate();
		newCoord.x = coord.x - (squareOffset * Math.cos(angle));
		newCoord.y = coord.y - (squareOffset * Math.sin(angle));
		
		return newCoord;
		
	}

	/**
	 * The robot travels to the Home zone, which its coordinates are fetched from the 
	 * competition's data
	 */
	public void travelToHomeZone() {
		double x,y;
		if (Main.TEAM_NUMBER == competitionData.builderTeamNumber){
			x = (competitionData.greenZone.lowerLeft.x + competitionData.greenZone.upperRight.x)/2;
			y = (competitionData.greenZone.lowerLeft.y + competitionData.greenZone.upperRight.y)/2;
		}else{
			x = (competitionData.greenZone.lowerLeft.x + competitionData.redZone.upperRight.x)/2;
			y = (competitionData.greenZone.lowerLeft.y + competitionData.redZone.upperRight.y)/2;
		}
		travelTo(x, y);
	}

	/**
	 * The robot will travel to the x and y coordinates
	 * 
	 * @param x 
	 * @param y 
	 */
	public void travelTo(double x, double y) {

		boolean traveling = true;

		while (traveling) {
			double distX = x - odometer.getX();
			double distY = y - odometer.getY();
			double distanceFromTarget = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));

			if (distanceFromTarget < acceptableError) {
				navigation.stopMoving();
				traveling = false;
				continue;
			}

			// Calculate the angle the plant needs to face in order to get to
			// the target
			double angle = Math.toDegrees(Math.atan2(distY, distX));

			// Turn only if the minimal angle to turn is larger than 50 degrees
			// (in any direction)
			// Prevents the plant from doing a lot of small turns that could
			// induce more error in the odometer.
			if (Navigation.minimalAngleDifference(odometer.getTheta(), angle) > acceptableError
					|| Navigation.minimalAngleDifference(odometer.getTheta(), angle) < -acceptableError) {
				navigation.turnTo(angle);
			}

			// After turning, go forward in the new direction.
			navigation.goForward();

			Helper.sleep(TIMEOUT_PERIOD);
		}

	}

}
