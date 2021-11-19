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
            int demoNum = Integer.parseInt(args[0]);

            switch (demoNum) {
            case 1: ir.rooms_and_rates(); break;
            case 2: ir.reservations(); break;
            case 3: ir.demo3(); break;
            case 4: ir.cancel_reservation(); break;
            case 5: ir.demo5(); break;
            }
            
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	} catch (Exception e2) {
            System.err.println("Exception: " + e2.getMessage());
        }
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

    private void rooms_and_rates() throws SQLException {

		System.out.println("FR1: Rooms and rates\r\n");

		init_connection();


		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
				String sql = "SELECT * FROM mjlong.lab7_rooms";


				String q3 = "with RoomPopularity AS ( SELECT Room, ROUND(SUM( CASE WHEN CheckIn >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut <= CURDATE() THEN DATEDIFF(CheckOut, CheckIn) WHEN CheckIn >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut > CURDATE() THEN DATEDIFF(CURDATE() , CheckIn) WHEN CheckIn < DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckOut <= CURDATE() THEN DATEDIFF(CheckOut, DATE_ADD(CURDATE(), INTERVAL -180 DAY)) ELSE 365 END)/180,2) AS occ_score FROM lab7_reservations WHERE (CheckOut >= DATE_ADD(CURDATE(), INTERVAL -180 DAY) AND CheckIn <= CURDATE() ) OR (CheckOut > CURDATE() AND CheckIn <= CURDATE() ) GROUP BY Room ORDER BY occ_score DESC ), nextAvailCheckIn AS ( SELECT Room, GREATEST(CURDATE(), MAX(CheckOut)) AS nextAvailableCheckIn FROM lab7_reservations GROUP BY Room ), MostRecentStay AS ( SELECT min_res.Room, DATEDIFF(CheckOut, CheckIn) AS Duration, CheckOut FROM ( SELECT Room, MIN(dt) AS mindt FROM ( SELECT Room, CheckIn, CheckOut, DATEDIFF(CURDATE(), Checkout) as dt FROM lab7_reservations WHERE DATEDIFF(CURDATE(), Checkout) > 0 ) recent_res GROUP BY Room ) min_res JOIN ( SELECT Room, CheckIn, CheckOut, DATEDIFF(CURDATE(), Checkout) as dt FROM lab7_reservations WHERE DATEDIFF(CURDATE(), Checkout) > 0 ) recent_res ON min_res.mindt = recent_res.dt AND recent_res.room = min_res.room ) SELECT rm.RoomCode, rm.RoomName, rm.Beds, rm.bedType, rm.maxOcc, rm.basePrice, rm.decor, occ_score, nextAvailableCheckIn, Duration, CheckOut FROM RoomPopularity AS rp JOIN nextAvailCheckIn AS nc ON rp.Room = nc.Room JOIN MostRecentStay AS mr ON mr.Room = rp.Room JOIN lab7_rooms AS rm ON RoomCode = mr.Room ORDER BY occ_score DESC";
			// Step 3: (omitted in this example) Start transaction

			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery(sql)) {

				// Step 5: Receive results
				while (rs.next()) {
					String RoomCode = rs.getString("RoomCode");
					String RoomName = rs.getString("RoomName");
					int Beds = rs.getInt("Beds");
					String bedType = rs.getString("bedType");
					int maxOcc = rs.getInt("maxOcc");
					int basePrice = rs.getInt("basePrice");
					String decor = rs.getString("decor");
					float occ_score = rs.getInt("occ_score"); // (.2%f)
					String next_avail = rs.getString("nextAvailableCheckIn");
					int Duration = rs.getInt("Duration");
					String CheckOut = rs.getString("CheckOut");
					System.out.format("%s, %s, %d, %n", RoomCode, RoomName, Beds);
				}
			}

			// Step 6: (omitted in this example) Commit or rollback transaction
		}


	}

	private void display_available_rooms(List<List<Object>> availableRooms) {
    	int i = 1;
    	for (List<Object> room : availableRooms) {
			System.out.format(
				"%d - %s %s %s %n",
				i,
				room.get(0),
				room.get(1),
				room.get(2)
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

	private int get_max_occ(Connection conn) throws SQLException {
		StringBuilder maxSb = new StringBuilder("SELECT MAX(maxOcc) AS max FROM mjlong.lab7_rooms");
		PreparedStatement maxPstmt = conn.prepareStatement(maxSb.toString());
		ResultSet maxRs = maxPstmt.executeQuery();
		maxRs.first();

		return Integer.parseInt(maxRs.getString("max"));
	}

	private int reservations() throws SQLException {
		System.out.println("\nFR2: Reservations\r\n");

		init_connection();


		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {

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
				return 0;
			}

			List<Object> params = new ArrayList<Object>();
			params.add(checkOut);
			params.add(checkIn);
			params.add(numChildren);
			params.add(numAdults);

			String availableRoomsQuery = (
				"SELECT DISTINCT RoomCode, bedType, decor, maxOcc FROM mjlong.lab7_reservations Reservations " +
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

			System.out.println(availableRooms);

			if (availableRooms.size() == 0) {
				System.out.println("No matches found for your requested date range.");
				return 0;
			} else {
				display_available_rooms(availableRooms);
			}

			System.out.format("----------------------%nFound %d %s%s %n", availableRooms.size(), matchType, availableRooms.size() == 1 ? "" : "es");


		}

		return 1;
	}


	private void cancel_reservation() throws SQLException {
		System.out.println("FR4: Cancel Reservation\r\n");
		init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter the code of the reservation you would like to cancel: ");

			int code = Integer.parseInt(scanner.nextLine());

			String fetch_reservation_query = ("SELECT * FROM lab7_reservations WHERE CODE = ?");
			String delete_row = ("DELETE FROM lab7_reservations WHERE CODE = ?");


			StringBuilder sb = new StringBuilder(delete_row);
			List<Object> params = new ArrayList<Object>();
			params.add(code);

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
					System.out.format("No reservation found with code %d", code);
				}

					
			}
			
		}

	}


    // Demo1 - Establish JDBC connection, execute DDL statement
    private void demo1() throws SQLException {

        System.out.println("demo1: Add AvailUntil column to hp_goods table\r\n");
        
	// Step 0: Load MySQL JDBC Driver
	// No longer required as of JDBC 2.0  / Java 6
	try{
	    Class.forName("com.mysql.jdbc.Driver");
	    System.out.println("MySQL JDBC Driver loaded");
	} catch (ClassNotFoundException ex) {
	    System.err.println("Unable to load JDBC Driver");
	    System.exit(-1);
	}

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							   System.getenv("HP_JDBC_USER"),
							   System.getenv("HP_JDBC_PW"))) {
	    // Step 2: Construct SQL statement
	    String sql = "ALTER TABLE hp_goods ADD COLUMN AvailUntil DATE";

	    // Step 3: (omitted in this example) Start transaction

	    try (Statement stmt = conn.createStatement()) {

		// Step 4: Send SQL statement to DBMS
		boolean exRes = stmt.execute(sql);
		
		// Step 5: Handle results
		System.out.format("Result from ALTER: %b %n", exRes);
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	catch (Exception e){
		System.err.println(e);
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }
    

    // Demo2 - Establish JDBC connection, execute SELECT query, read & print result
    private void demo2() throws SQLException {

        System.out.println("demo2: List content of hp_goods table\r\n");
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							   System.getenv("HP_JDBC_USER"),
							   System.getenv("HP_JDBC_PW"))) {
	    // Step 2: Construct SQL statement
	    String sql = "SELECT * FROM hp_goods";

	    // Step 3: (omitted in this example) Start transaction

	    // Step 4: Send SQL statement to DBMS
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

		// Step 5: Receive results
		while (rs.next()) {
		    String flavor = rs.getString("Flavor");
		    String food = rs.getString("Food");
		    float price = rs.getFloat("price");
		    System.out.format("%s %s ($%.2f) %n", flavor, food, price);
		}
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }


    // Demo3 - Establish JDBC connection, execute DML query (UPDATE)
    // -------------------------------------------
    // Never (ever) write database code like this!
    // -------------------------------------------
    private void demo3() throws SQLException {

        System.out.println("demo3: Populate AvailUntil column using string concatenation\r\n");
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							   System.getenv("HP_JDBC_USER"),
							   System.getenv("HP_JDBC_PW"))) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    String availUntilDate = scanner.nextLine();

	    // -------------------------------------------
	    // Never (ever) write database code like this!
	    // -------------------------------------------
	    String updateSql = "UPDATE hp_goods SET AvailUntil = '" + availUntilDate + "' " +
		               "WHERE Flavor = '" + flavor + "'";

	    // Step 3: (omitted in this example) Start transaction
	    
	    try (Statement stmt = conn.createStatement()) {
		
		// Step 4: Send SQL statement to DBMS
		int rowCount = stmt.executeUpdate(updateSql);
		
		// Step 5: Handle results
		System.out.format("Updated all '%s' flavored pastries (%d records) %n", flavor, rowCount);		
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	    
	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }


    // Demo4 - Establish JDBC connection, execute DML query (UPDATE) using PreparedStatement / transaction    
    private void demo4() throws SQLException {

        System.out.println("demo4: Populate AvailUntil column using PreparedStatement\r\n");
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							   System.getenv("HP_JDBC_USER"),
							   System.getenv("HP_JDBC_PW"))) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    LocalDate availDt = LocalDate.parse(scanner.nextLine());
	    
	    String updateSql = "UPDATE hp_goods SET AvailUntil = ? WHERE Flavor = ?";

	    // Step 3: Start transaction
	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
		
		// Step 4: Send SQL statement to DBMS
		pstmt.setDate(1, java.sql.Date.valueOf(availDt));
		pstmt.setString(2, flavor);
		int rowCount = pstmt.executeUpdate();
		
		// Step 5: Handle results
		System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);

		// Step 6: Commit or rollback transaction
		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }

	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }



    // Demo5 - Construct a query using PreparedStatement
    private void demo5() throws SQLException {

        System.out.println("demo5: Run SELECT query using PreparedStatement\r\n");
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							   System.getenv("HP_JDBC_USER"),
							   System.getenv("HP_JDBC_PW"))) {
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Find pastries with price <=: ");
	    Double price = Double.valueOf(scanner.nextLine());
	    System.out.print("Filter by flavor (or 'Any'): ");
	    String flavor = scanner.nextLine();

	    List<Object> params = new ArrayList<Object>();
	    params.add(price);
	    StringBuilder sb = new StringBuilder("SELECT * FROM goods WHERE price <= ?");
	    if (!"any".equalsIgnoreCase(flavor)) {
		sb.append(" AND Flavor = ?");
		params.add(flavor);
	    }
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
		int i = 1;
		for (Object p : params) {
		    pstmt.setObject(i++, p);
		}

		try (ResultSet rs = pstmt.executeQuery()) {
		    System.out.println("Matching Pastries:");
		    int matchCount = 0;
		    while (rs.next()) {
			System.out.format("%s %s ($%.2f) %n", rs.getString("Flavor"), rs.getString("Food"), rs.getDouble("price"));
			matchCount++;
		    }
		    System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
		}
	    }

	}
    }
    

}
