import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Scanner;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/*
Introductory JDBC examples based loosely on the BAKERY dataset from CSC 365 labs.

-- MySQL setup:
drop table if exists hp_goods, hp_customers, hp_items, hp_receipts;
create table hp_goods as select * from BAKERY.goods;
create table hp_customers as select * from BAKERY.customers;
create table hp_items as select * from BAKERY.items;
create table hp_receipts as select * from BAKERY.receipts;

grant all on mjlong.hp_goods to hasty@'%';
grant all on mjlong.hp_customers to hasty@'%';
grant all on mjlong.hp_items to hasty@'%';
grant all on mjlong.hp_receipts to hasty@'%';

-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/hpigg?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=hpigg
export HP_JDBC_PW=csc365-F2021_025362993

export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/mjlong?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=mjlong
export HP_JDBC_PW=csc365-F2021_013777227

 */
public class InnReservations {
	public static void main(String[] args) {
		try {
			InnReservations ir = new InnReservations();

			ir.print_help();

			Scanner scanner = new Scanner(System.in);

			System.out.printf("> ");
			String command = scanner.nextLine();

			while (!"q".equals(command)) {
				switch(command) {
					case "fr1": ir.rooms_and_rates(); break;
					case "fr2": ir.reservations(); break;
					case "fr3": ir.change_reservation(); break;
					case "fr4": ir.cancel_reservation(); break;
					case "fr5": ir.detailed_reservations(); break;
					case "fr6": ir.revenue(); break;
					default:
						System.out.println("Invalid Command");
						break;
				}
				ir.print_help();
				System.out.printf("> ");
				command = scanner.nextLine();
			}
		} catch (SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
		} catch (Exception e2) {
			System.err.println("Exception: " + e2.getMessage());
		}
	}

	private void print_help() {
		System.out.println(
				"\nMain Menu\n\n" +
				"commands:\n" +
				"    fr1\n" +
				"        - List rooms by popularity\n" +
				"    fr2\n" +
				"        - Book a room\n" +
				"    fr3\n" +
				"        - Make a change to an existing reservation\n" +
				"    fr4\n" +
				"        - Cancel an existing reservation\n" +
				"    fr5\n" +
				"        - Get detailed reservation information\n" +
				"    fr6\n" +
				"        - Get month-by-month revenue for a given year\n" +
				"    q\n" +
				"        - Quit the CLI\n"
		);
	}

	private static void init_connection() throws SQLException {

		try{
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("MySQL JDBC Driver loaded");
		} catch (ClassNotFoundException ex) {
			System.err.println("Unable to load JDBC Driver");
			System.exit(-1);
		}

	}

	private int rooms_and_rates() throws SQLException {

		System.out.println("FR1: Rooms and rates\r\n");

		init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			String q3 = "with RoomPopularity AS ( SELECT Room, ROUND(SUM( CASE WHEN CheckIn >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut <= CURDATE() THEN DATEDIFF(CheckOut, CheckIn) WHEN CheckIn >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut > CURDATE() THEN DATEDIFF(CURDATE() , CheckIn) WHEN CheckIn < DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut <= CURDATE() THEN DATEDIFF(CheckOut, DATE_ADD(CURDATE(), INTERVAL -180 DAY)) ELSE 365 END)/180,2) AS occ_score FROM mjlong.lab7_reservations WHERE (CheckOut >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckIn <= CURDATE() ) OR (CheckOut > CURDATE() AND CheckIn <= CURDATE() ) GROUP BY Room ORDER BY occ_score DESC ), nextAvailCheckIn AS ( SELECT Room, GREATEST(CURDATE(), MAX(CheckOut)) AS nextAvailableCheckIn FROM mjlong.lab7_reservations GROUP BY Room ), MostRecentStay AS ( SELECT min_res.Room, DATEDIFF(CheckOut, CheckIn) AS Duration, CheckOut FROM ( SELECT Room, MIN(dt) AS mindt FROM ( SELECT Room, CheckIn, CheckOut, DATEDIFF(CURDATE(), Checkout) as dt FROM mjlong.lab7_reservations WHERE DATEDIFF(CURDATE(), Checkout) > 0 ) recent_res GROUP BY Room ) min_res JOIN ( SELECT Room, CheckIn, CheckOut, DATEDIFF(CURDATE(), Checkout) as dt FROM mjlong.lab7_reservations WHERE DATEDIFF(CURDATE(), Checkout) > 0 ) recent_res ON min_res.mindt = recent_res.dt AND recent_res.room = min_res.room ) SELECT rm.RoomCode, rm.RoomName, rm.Beds, rm.bedType, rm.maxOcc, rm.basePrice, rm.decor, occ_score, nextAvailableCheckIn, Duration, CheckOut FROM RoomPopularity AS rp JOIN nextAvailCheckIn AS nc ON rp.Room = nc.Room JOIN MostRecentStay AS mr ON mr.Room = rp.Room JOIN mjlong.lab7_rooms AS rm ON RoomCode = mr.Room ORDER BY occ_score DESC";

			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery(q3)) {

				System.out.println("RoomCode,RoomName,Beds,bedType,maxOcc,basePrice,decor,occ_score,nextAvailableCheckIn,Duration,CheckOut");
				while (rs.next()) {
					System.out.format(
							"%s, %s, %d, %s, %d, %d, %s, %.2f, %s, %d %s %n",
							rs.getString("RoomCode"),
							rs.getString("RoomName"),
							rs.getInt("Beds"),
							rs.getString("bedType"),
							rs.getInt("maxOcc"),
							rs.getInt("basePrice"),
							rs.getString("decor"),
							rs.getFloat("occ_score"),
							rs.getString("nextAvailableCheckIn"),
							rs.getInt("Duration"),
							rs.getString("CheckOut")
					);
				}
			}

			return 0;
		}
	}

	private List<Integer> count_weekdays_and_weekends(Date start, Date end) {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(start);

		Calendar endCal = Calendar.getInstance();
		endCal.setTime(end);

		List<Integer> weekendsAndWeekdays = new ArrayList<Integer>();
		weekendsAndWeekdays.add(0);
		weekendsAndWeekdays.add(0);

		do {
			if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
				weekendsAndWeekdays.set(1, weekendsAndWeekdays.get(1) + 1);
			} else {
				weekendsAndWeekdays.set(0, weekendsAndWeekdays.get(0) + 1);
			}
			startCal.add(Calendar.DAY_OF_MONTH, 1);
		} while (startCal.getTimeInMillis() <= endCal.getTimeInMillis());

		return weekendsAndWeekdays;
	}

	private void display_available_rooms(List<List<Object>> availableRooms) {
		int i = 1;
		for (List<Object> room : availableRooms) {
			System.out.format(
					"%d - %s - %s, %s, %s %n",
					i,
					room.get(0),
					room.get(1),
					room.get(2),
					room.get(3)
			);
			i++;
		}
	}

	private List<List<Object>> query_for_room_matches(StringBuilder sb, List<Object> params, Connection conn) throws SQLException {
		int matchCount = 0;
		List<List<Object>> availableRooms = new ArrayList<List<Object>>();
		try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
			int i = 1;
			for (Object p : params) {
				pstmt.setObject(i++, p);
			}

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next() && matchCount < 5) {
					if (matchCount == 0) {
						System.out.format("%nAvailable Rooms:%n");
					}
					availableRooms.add(
							new ArrayList<Object>(Arrays.asList(
									rs.getString("RoomCode"),
									rs.getString("RoomName"),
									rs.getString("bedType"),
									rs.getString("decor")
							))
					);

					matchCount++;
				}
			}
		}

		return availableRooms;
	}

	private Double get_base_room_price(String RoomCode, Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT basePrice FROM mjlong.lab7_rooms WHERE RoomCode = ?");
		pstmt.setObject(1, RoomCode);
		ResultSet rs = pstmt.executeQuery();
		rs.first();

		return rs.getDouble("basePrice");
	}

	private int get_max_reservation_id(Connection conn) throws SQLException {
		StringBuilder maxSb = new StringBuilder("SELECT MAX(CODE) AS max FROM mjlong.lab7_reservations");
		PreparedStatement maxPstmt = conn.prepareStatement(maxSb.toString());
		ResultSet maxRs = maxPstmt.executeQuery();
		maxRs.first();

		return Integer.parseInt(maxRs.getString("max"));
	}

	private int select_room_reservation(
			Connection conn,
			int selection,
			List<List<Object>> availableRooms,
			String firstName,
			String lastName,
			String checkIn,
			String checkOut,
			int numAdults,
			int numChildren
	) throws SQLException, ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date checkInDate = format.parse(checkIn);
		Date checkOutDate = format.parse(checkOut);

		List<Integer> weekendsAndWeekdays = count_weekdays_and_weekends(checkInDate, checkOutDate);

		Double basePrice = get_base_room_price(availableRooms.get(selection - 1).get(0).toString(), conn);

		Double totalCost = basePrice * weekendsAndWeekdays.get(1) + basePrice * weekendsAndWeekdays.get(0) * 1.1;

		System.out.println("Please confirm this is the room you would like to reserve:\n");
		System.out.format(
				"Name: %s, %s\n" +
				"Room Description: %s - %s, %s \n" +
				"Dates: %s - %s\n" +
				"Adults: %s\n" +
				"Children: %s\n" +
				"Total Cost of Stay: $%,.2f\n\n",
				firstName,
				lastName,
				availableRooms.get(selection - 1).get(0),
				availableRooms.get(selection - 1).get(1),
				availableRooms.get(selection - 1).get(2),
				checkIn,
				checkOut,
				numAdults,
				numChildren,
				totalCost
		);

		System.out.println("Type 'CONFIRM' to confirm or 'q' to return to the main menu: ");
		Scanner scanner = new Scanner(System.in);

		String choice = scanner.nextLine();

		while (!"q".equalsIgnoreCase(choice) && !"CONFIRM".equals(choice)) {
			System.out.println("Invalid selection. Please type 'CONFIRM' to confirm or 'q' to cancel: ");
			choice = scanner.nextLine();
		}
		if ("q".equalsIgnoreCase(choice)) {
			return 1;
		}

		int newReservationCode = get_max_reservation_id(conn) + 1;

		conn.setAutoCommit(false);

		try {
			PreparedStatement insertPstmt = conn.prepareStatement(String.format(
					"INSERT INTO mjlong.`lab7_reservations` " +
							"(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids)" +
							"VALUES (%d, '%s', '%s', '%s', %,.2f, '%s', '%s', %d, %d)",
					newReservationCode,
					availableRooms.get(selection - 1).get(0),
					checkIn,
					checkOut,
					totalCost / (weekendsAndWeekdays.get(0) + weekendsAndWeekdays.get(1)),
					lastName.toUpperCase(),
					firstName.toUpperCase(),
					numAdults,
					numChildren
			));

			int updatedRows = insertPstmt.executeUpdate();

			if (updatedRows > 0) {
				System.out.println("Successfully booked your reservation. See you then!");
			} else {
				System.out.println("ERROR: Could not book your reservation.");
			}

			conn.commit();
			return 0;
		} catch (SQLException e) {
			conn.rollback();
			return 1;
		}
	}

	private int get_max_occ(Connection conn) throws SQLException {
		StringBuilder maxSb = new StringBuilder("SELECT MAX(maxOcc) AS max FROM mjlong.lab7_rooms");
		PreparedStatement maxPstmt = conn.prepareStatement(maxSb.toString());
		ResultSet maxRs = maxPstmt.executeQuery();
		maxRs.first();

		return Integer.parseInt(maxRs.getString("max"));
	}

	private int reservations() throws SQLException, ParseException {
		System.out.println("\nFR2: Reservations\r\n");

		init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			conn.setAutoCommit(false);

			get_max_occ(conn);
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter your first name: ");
			String firstName = scanner.nextLine();
			System.out.print("Enter your last name: ");
			String lastName = scanner.nextLine();
			System.out.print("Enter desired room code (or 'Any'): ");
			String roomCode = scanner.nextLine();
			System.out.print("Enter desired bed type (or 'Any': ");
			String bedType = scanner.nextLine();
			System.out.print("When would you like to check in? (YYYY-MM-DD): ");
			String checkIn = scanner.nextLine();
			System.out.print("When would you like to check out? (YYYY-MM-DD): ");
			String checkOut = scanner.nextLine();
			System.out.print("How many children in your party? ");
			int numChildren = Integer.parseInt(scanner.nextLine());
			System.out.print("How many adults in your party? ");
			int numAdults = Integer.parseInt(scanner.nextLine());

			if (numChildren + numAdults > get_max_occ(conn)) {
				System.out.print("Your party size is greater than our maximum room size. No rooms are available matching this occupancy.\n");
				return 1;
			}

			List<Object> params = new ArrayList<Object>();
			params.add(checkOut);
			params.add(checkIn);
			params.add(numChildren);
			params.add(numAdults);

			String availableRoomsQuery = (
				"SELECT DISTINCT RoomCode, RoomName, bedType, decor FROM mjlong.lab7_reservations Reservations " +
					"JOIN mjlong.lab7_rooms Rooms ON Rooms.RoomCode = Reservations.Room " +
					"WHERE Rooms.RoomCode NOT IN (" +
						"SELECT r2.Room " +
							"FROM mjlong.lab7_reservations r2 " +
							"WHERE r2.CheckIn < ? AND r2.CheckOut > ? " +
					") AND ? + ? <= Rooms.maxOcc"
			);

			// Build query string
			StringBuilder exactSb = new StringBuilder(availableRoomsQuery);

			if (!"any".equalsIgnoreCase(roomCode)) {
				exactSb.append(" AND RoomCode = ?");
				params.add(roomCode);
			}
			if (!"any".equalsIgnoreCase(bedType)) {
				exactSb.append(" AND bedType = ?");
				params.add(bedType);
			}

			String matchType = "exact match";

			List<List<Object>> availableRooms = query_for_room_matches(exactSb, params, conn);

			if (availableRooms.size() == 0) {
				boolean flag = false;
				while (params.size() > 4) {
					params.remove(4);
				}

				matchType = "close match";

				StringBuilder similarSb = new StringBuilder(availableRoomsQuery);

				String operator = " AND (";

				if (!"any".equalsIgnoreCase(bedType)) {
					flag = true;
					similarSb.append(operator);
					similarSb.append(" bedType = ?");
					params.add(bedType);
					operator = " OR ";
				}
				if (!"any".equalsIgnoreCase(roomCode)) {
					flag = true;
					similarSb.append(operator);
					similarSb.append(" decor IN (SELECT decor FROM mjlong.lab7_rooms WHERE RoomCode = ?)");
					params.add(roomCode);
				}
				if (flag) {
					similarSb.append(")");
				}

				availableRooms = query_for_room_matches(similarSb, params, conn);
			}

			if (availableRooms.size() == 0) {
				System.out.println("No matches found for your requested date range.");
				return 1;
			} else {
				display_available_rooms(availableRooms);
			}

			System.out.format("----------------------%nFound %d %s%s %n", availableRooms.size(), matchType, availableRooms.size() == 1 ? "" : "es");

			System.out.println("\nSelect one of the room options by number or 'q' to cancel reservation: ");
			String selection = scanner.nextLine();
			while (!"q".equalsIgnoreCase(selection) && (Integer.parseInt(selection) < 1 || Integer.parseInt(selection) > availableRooms.size())) {
				System.out.println("Invalid selection. Please select one of the reservation numbers above or 'q' to cancel: ");
				selection = scanner.nextLine();
			}
			if ("q".equalsIgnoreCase(selection)) {
				return 1;
			}

			return select_room_reservation(
					conn,
					Integer.parseInt(selection),
					availableRooms,
					firstName,
					lastName,
					checkIn,
					checkOut,
					numAdults,
					numChildren
			);
		}
	}

	private boolean check_for_conflicting_reservations(
			Connection conn,
			String reservationCode,
			String roomCode,
			String checkIn,
			String checkOut,
			String numChildren,
			String numAdults
	) throws SQLException {
		StringBuilder sb = new StringBuilder(
				String.format("SELECT DISTINCT RoomCode FROM mjlong.lab7_rooms Rooms " +
						"WHERE Rooms.RoomCode NOT IN (" +
							"SELECT r2.Room " +
								"FROM mjlong.lab7_reservations r2 " +
								"WHERE r2.CheckIn < '%s' AND r2.Checkout > '%s' AND NOT r2.CODE = '%s' " +
						") AND %s + %s <= Rooms.maxOcc " +
						"AND Rooms.RoomCode = '%s'",
						checkOut,
						checkIn,
						reservationCode,
						numChildren,
						numAdults,
						roomCode
				)
		);

		PreparedStatement pstmt = conn.prepareStatement(sb.toString());

		ResultSet rs = pstmt.executeQuery();

		if (rs.next() == false) {
			System.out.println("ERROR: reservation change conflicts with existing reservation.");
			return false;
		} else {
			return true;
		}
	}

	private ResultSet get_reservation(Connection conn, String code) throws SQLException {
		StringBuilder sb = new StringBuilder(
			"SELECT * FROM mjlong.lab7_reservations " +
				"WHERE CODE = ?"
		);

		PreparedStatement pstmt = conn.prepareStatement(sb.toString());

		pstmt.setObject(1, code);

		ResultSet rs = pstmt.executeQuery();

		return rs;
	}

	private int update_reservation(Connection conn, String code, String updateString, List<Object> params) throws SQLException {
		StringBuilder sb = new StringBuilder(
				String.format("UPDATE mjlong.lab7_reservations " +
						"SET %s " +
						"WHERE CODE = ?",
						updateString
				)
		);

		conn.setAutoCommit(false);

		try {
			PreparedStatement pstmt = conn.prepareStatement(sb.toString());

			int i = 1;
			for (Object p : params) {
				pstmt.setObject(i++, p);
			}
			pstmt.setObject(i, code);

			int updatedRows = pstmt.executeUpdate();

			if (updatedRows == 0) {
				System.out.println("Unable to update reservation.");
			} else {
				System.out.format("Successfully updated reservation %s.", code);
			}

			conn.commit();
			return 0;
		} catch (SQLException e) {
			conn.rollback();
			return 1;
		}
	}

	private int change_reservation() throws SQLException {
		String roomCode = new String();
		String firstName = new String();
		String lastName = new String();
		String checkIn = new String();
		String checkOut = new String();
		String childCount = new String();
		String adultCount = new String();

		System.out.println("FR3: Reservation Change\n");
		init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter the code of your reservation: ");
			String reservationCode = scanner.nextLine();
			System.out.print("Enter new first name (or 'no change' to leave unchanged): ");
			String newFirstName = scanner.nextLine();
			System.out.print("Enter new last name (or 'no change' to leave unchanged): ");
			String newLastName = scanner.nextLine();
			System.out.print("Enter new check in date (or 'no change' to leave unchanged): ");
			String newCheckIn = scanner.nextLine();
			System.out.print("Enter new check out date (or 'no change' to leave unchanged): ");
			String newCheckOut = scanner.nextLine();
			System.out.print("Enter new child count (or 'no change' to leave unchanged): ");
			String newChildCount = scanner.nextLine();
			System.out.print("Enter new adult count (or 'no change' to leave unchanged): ");
			String newAdultCount = scanner.nextLine();

			String updateString = new String();

			ResultSet rs = get_reservation(conn, reservationCode);
			if (rs.first() == false) {
				System.out.println("No matching reservation found.");
				return 1;
			} else {
				roomCode = rs.getString("Room");
				firstName = rs.getString("FirstName");
				lastName = rs.getString("LastName");
				checkIn = rs.getString("CheckIn");
				checkOut = rs.getString("Checkout");
				childCount = rs.getString("Kids");
				adultCount = rs.getString("Adults");
			}

			List<Object> params = new ArrayList<Object>();

			if (!"no change".equalsIgnoreCase(newFirstName)) {
				updateString = updateString + ("FirstName = ?, ");
				firstName = newFirstName;
				params.add(newFirstName);
			}
			if (!"no change".equalsIgnoreCase(newLastName)) {
				updateString = updateString + ("LastName = ?, ");
				lastName = newLastName;
				params.add(newLastName);
			}
			if (!"no change".equalsIgnoreCase(newCheckIn)) {
				updateString = updateString + ("CheckIn = ?, ");
				checkIn = newCheckIn;
				params.add(newCheckIn);
			}
			if (!"no change".equalsIgnoreCase(newCheckOut)) {
				updateString = updateString + ("Checkout = ?, ");
				checkOut = newCheckOut;
				params.add(newCheckOut);
			}
			if (!"no change".equalsIgnoreCase(newChildCount)) {
				updateString = updateString + (String.format("Kids = ?, "));
				childCount = newChildCount;
				params.add(newChildCount);
			}
			if (!"no change".equalsIgnoreCase(newAdultCount)) {
				updateString = updateString + ("Adults = ?, ");
				adultCount = newAdultCount;
				params.add(newAdultCount);
			}
			updateString = updateString.substring(0, updateString.length() - 2);

			if (check_for_conflicting_reservations(conn, reservationCode, roomCode, checkIn, checkOut, childCount, adultCount)) {
				return update_reservation(conn, reservationCode, updateString, params);
			} else {
				return 1;
			}
		}
	}

	private int cancel_reservation() throws SQLException {
		System.out.println("FR4: Cancel Reservation\r\n");
		init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter the code of the reservation you would like to cancel: ");

			int code = Integer.parseInt(scanner.nextLine());

			String fetch_reservation_query = ("SELECT * FROM mjlong.lab7_reservations WHERE CODE = ?");
			String delete_row = ("DELETE FROM mjlong.lab7_reservations WHERE CODE = ?");


			StringBuilder sb = new StringBuilder(delete_row);
			List<Object> params = new ArrayList<Object>();
			params.add(code);

			conn.setAutoCommit(false);

			// Execute Query
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				int r_deleted = pstmt.executeUpdate();

				if (r_deleted == 1){
					System.out.println("Reservation successfully deleted");
				}
				else {
					System.out.format("No reservation found with code %d %n", code);
				}

				conn.commit();
				return 0;
			} catch (SQLException e) {
				conn.rollback();
				return 1;
			}
		}
	}

	private void detailed_reservations() throws SQLException {
		System.out.println("FR5: Detailed reservation information\r\n");

		init_connection();

		//int reservationCode = 0

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter first name: ");
			String firstName = scanner.nextLine();
			System.out.print("Enter last name: ");
			String lastName = scanner.nextLine();
			System.out.print("Enter first date where all reservations must be greater than (or equal to): ");
			String dateRange1 = scanner.nextLine();
			System.out.print("Enter second date where all reservations must be less than (or equal to): ");
			String dateRange2 = scanner.nextLine();
			System.out.print("Enter desired room code: ");
			String roomCode = scanner.nextLine();
			System.out.print("Enter desired reservation code: ");
			String reservationCode = scanner.nextLine();
			
			String baseQuery = (
				"SELECT RoomName, mjlong.lab7_reservations.* FROM mjlong.lab7_reservations JOIN mjlong.lab7_rooms ON Room = RoomCode WHERE FirstName LIKE ? AND LastName LIKE ? AND Room LIKE ? AND CODE LIKE ?");

			// Build query string
			StringBuilder sb = new StringBuilder(baseQuery);
			
			List<Object> params = new ArrayList<Object>();

			firstName = firstName + "%";
			lastName = lastName + "%";
			roomCode = roomCode + "%";
			reservationCode = reservationCode + "%";

			params.add(firstName);
			params.add(lastName);
			params.add(roomCode);
			params.add(reservationCode);

			// both dates are specified
			if (!dateRange1.equals("") && !dateRange2.equals("")) {
				sb.append(" AND ((CheckIn >= ? AND CheckOut <= ?) OR (CheckIn <= ? AND CheckOut >= ?))");
				params.add(dateRange1);
				params.add(dateRange2);
				params.add(dateRange2);
				params.add(dateRange1);

			}
			// only first date is specified
			else if (!dateRange1.equals("") && dateRange2.equals("")) {
				sb.append(" AND (CheckIn >= ? OR CheckOut >= ?)");
				params.add(dateRange1);
				params.add(dateRange1);

			}
			else if (dateRange1.equals("") && !dateRange2.equals("")) {
				sb.append(" AND (CheckIn <= ? OR CheckOut <= ?)");
				params.add(dateRange2);
				params.add(dateRange2);
			}
		
			// Execute Query
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.format("%nReservations:%n");
					System.out.println("RoomName,CODE,Room,CheckIn,CheckOut,Rate,LastName,FirstName,Adults,Kids");
					while (rs.next()) {
						System.out.format(
								"%s, %d, %s, %s, %s, ($%.2f), %s, %s, %d, %d %n",
								rs.getString("RoomName"),
								rs.getInt("CODE"),
								rs.getString("Room"),
								rs.getString("CheckIn"),
								rs.getString("CheckOut"),
								rs.getFloat("Rate"),
								rs.getString("LastName"),
								rs.getString("FirstName"),
								rs.getInt("Adults"),
								rs.getInt("Kids")
						);
					}

					
				}

			}
		}
	}

	private void revenue() throws SQLException {
		System.out.println("FR6: Detailed revenue per year\r\n");

		init_connection();

		//int reservationCode = 0

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter desired year for revenue: ");
			String year = scanner.nextLine();

			String firstDate = year + "-01-01";
			String secondDate = year + "-12-31";
			
			String monthQuery = "with raw_data AS ( with dates (selected_date) AS (SELECT selected_date FROM (SELECT adddate('2010-01-01',time4.i*10000 + time3.i*1000 + time2.i*100 + time1.i*10 + time0.i) selected_date FROM (SELECT 0 i union select 1 union select 2 union select 3 union SELECT 4 union select 5 union select 6 union select 7 union SELECT 8 union select 9) time0, (SELECT 0 i union select 1 union select 2 union select 3 union SELECT 4 union select 5 union select 6 union select 7 union SELECT 8 union select 9) time1, (SELECT 0 i union select 1 union select 2 union select 3 union SELECT 4 union select 5 union select 6 union select 7 union SELECT 8 union select 9) time2, (SELECT 0 i union select 1 union select 2 union select 3 union SELECT 4 union select 5 union select 6 union select 7 union SELECT 8 union select 9) time3, (SELECT 0 i union select 1 union select 2 union select 3 union SELECT 4 union select 5 union select 6 union select 7 union SELECT 8 union select 9) time4) v where selected_date between ? and ? order by selected_date ), rooms (Room) as (select distinct Room from mjlong.lab7_reservations) select distinct rooms.Room, dates.selected_date, IFNULL((select sum(r.rate) from mjlong.lab7_reservations r where dates.selected_date between checkIn and (checkOut - 1) and r.Room=rooms.Room group by dates.selected_date), 0) rate from dates, rooms, mjlong.lab7_reservations r1 where rooms.Room = r1.Room order by dates.selected_date ), revenue AS ( SELECT Room, ROUND(SUM( CASE WHEN MONTH(selected_date) = 1 THEN rate ELSE 0 END),0) AS January, ROUND(SUM( CASE WHEN MONTH(selected_date) = 2 THEN rate ELSE 0 END),0) AS February, ROUND(SUM( CASE WHEN MONTH(selected_date) = 3 THEN rate ELSE 0 END),0) AS March, ROUND(SUM( CASE WHEN MONTH(selected_date) = 4 THEN rate ELSE 0 END),0) AS April, ROUND(SUM( CASE WHEN MONTH(selected_date) = 5 THEN rate ELSE 0 END),0) AS May, ROUND(SUM( CASE WHEN MONTH(selected_date) = 6 THEN rate ELSE 0 END),0) AS June, ROUND(SUM( CASE WHEN MONTH(selected_date) = 7 THEN rate ELSE 0 END),0) AS July, ROUND(SUM( CASE WHEN MONTH(selected_date) = 8 THEN rate ELSE 0 END),0) AS August, ROUND(SUM( CASE WHEN MONTH(selected_date) = 9 THEN rate ELSE 0 END),0) AS September, ROUND(SUM( CASE WHEN MONTH(selected_date) = 10 THEN rate ELSE 0 END),0) AS October, ROUND(SUM( CASE WHEN MONTH(selected_date) = 11 THEN rate ELSE 0 END),0) AS November, ROUND(SUM( CASE WHEN MONTH(selected_date) = 12 THEN rate ELSE 0 END),0) AS December, ROUND(SUM(rate),0) AS Yearly FROM raw_data GROUP BY Room ) select Room, January, February, March, April, May, June, July, August, September, October, November, December, Yearly FROM revenue";
			// Build query string
			StringBuilder sb = new StringBuilder(monthQuery);
			
			List<Object> params = new ArrayList<Object>();

			params.add(firstDate);
			params.add(secondDate);
		
			// Execute Query
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.format("%nReservations:%n");
					System.out.println("Room, January, February, March, April, May, June, July, August, September, October, November, December");
					while (rs.next()) {
						System.out.format(
								"%s,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %d,  %n",
								rs.getString("Room"),
								rs.getInt("January"),
								rs.getInt("February"),
								rs.getInt("March"),
								rs.getInt("April"),
								rs.getInt("May"),
								rs.getInt("June"),
								rs.getInt("July"),
								rs.getInt("August"),
								rs.getInt("September"),
								rs.getInt("October"),
								rs.getInt("November"),
								rs.getInt("December"),
								rs.getInt("Yearly")
						);
					}

					
				}

			}
		}

	}

}