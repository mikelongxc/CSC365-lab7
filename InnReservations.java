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
            case 5: ir.detailed_reservations(); break;
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

	private void reservations() throws SQLException {
    	System.out.println("FR2: Reservations\r\n");

    	init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
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

			String availableRoomsQuery = (
				"SELECT DISTINCT RoomCode, bedType, decor FROM mjlong.lab7_reservations Reservations " +
				"JOIN mjlong.lab7_rooms Rooms ON Rooms.RoomCode = Reservations.Room " +
				"WHERE (CheckIn > ? OR CheckOut < ?) AND ? + ? <= Rooms.maxOcc"
			);

			// Build query string
			StringBuilder sb = new StringBuilder(availableRoomsQuery);

			List<Object> params = new ArrayList<Object>();
			params.add(checkIn);
			params.add(checkOut);
			params.add(numChildren);
			params.add(numAdults);
			if (!"any".equalsIgnoreCase(roomCode)) {
				sb.append(" AND RoomCode = ?");
				params.add(roomCode);
			}
			if (!"any".equalsIgnoreCase(bedType)) {
				sb.append(" AND bedType = ?");
				params.add(bedType);
			}

			// Execute Query
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.format("%nAvailable Rooms:%n");
					int matchCount = 0;
					while (rs.next()) {
						System.out.format(
								"%s %s %s %n",
								rs.getString("RoomCode"),
								rs.getString("bedType"),
								rs.getString("decor")
						);
						matchCount++;
					}

					if (matchCount == 0) {
						PreparedStatement similarpstmt = conn.prepareStatement(
								new StringBuilder(availableRoomsQuery).toString()
						);
						i = 1;
						for (Object p : params) {
							if (i < 5) {
								similarpstmt.setObject(i++, p);
							}
						}

						try(ResultSet similarrs = similarpstmt.executeQuery()) {
							System.out.format("No exact matches found. Here are some similar available rooms:%n");
							matchCount = 0;
							while (similarrs.next()) {
								System.out.format(
										"%s %s %s %n",
										similarrs.getString("RoomCode"),
										similarrs.getString("bedType"),
										similarrs.getString("decor")
								);
								matchCount++;
							}
						}
					}

					System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
				}
			}

			// Step 6: (omitted in this example) Commit or rollback transaction
		}
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
					System.out.format("No reservation found with code %d %n", code);
				}
			}
		}
	}

	private void detailed_reservations() throws SQLException {
		System.out.println("FR5: Detailed reservation information\r\n");

    	init_connection();

		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Get Query Params
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter first name: ");
			String firstName = scanner.nextLine();
			System.out.print("Enter last name: ");
			String lastName = scanner.nextLine();
			System.out.print("Enter first date in range: ");
			String dateRange1 = scanner.nextLine();
			System.out.print("Enter second date in range: ");
			String dateRange2 = scanner.nextLine();
			System.out.print("Enter desired room code: ");
			String roomCode = scanner.nextLine();
			System.out.print("Enter desired reservation code: ");
			String reservationCodeStr = scanner.nextLine();
			if (reservationCodeStr != "") {
				int reservationCode = Integer.parseInt(reservationCodeStr);
			}
			
			String baseQuery = (
				"SELECT RoomName, lab7_reservations.* FROM lab7_reservations JOIN lab7_rooms ON Room = RoomCode");

			// Build query string
			StringBuilder sb = new StringBuilder(baseQuery);
			
			List<Object> params = new ArrayList<Object>();

			params.add(firstName);
			params.add(lastName);
			params.add(dateRange1);
			params.add(dateRange2);
			params.add(roomCode);
			if (!"any".equalsIgnoreCase(firstName)) {
				sb.append(" AND FirstName = ?");
				params.add(firstName);
			} 

			// Execute Query
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.format("%nReservations:%n");
					int matchCount = 0;
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
						matchCount++;
					}

					if (matchCount == 0) {
						PreparedStatement similarpstmt = conn.prepareStatement(
								new StringBuilder(baseQuery).toString()
						);
						i = 1;
						for (Object p : params) {
							if (i < 5) {
								similarpstmt.setObject(i++, p);
							}
						}

						try(ResultSet similarrs = similarpstmt.executeQuery()) {
							System.out.format("No exact matches found. Here are some similar available rooms:%n");
							matchCount = 0;
							while (similarrs.next()) {
								System.out.format(
										"%s %s %s %n",
										similarrs.getString("RoomCode"),
										similarrs.getString("bedType"),
										similarrs.getString("decor")
								);
								matchCount++;
							}
						}
					}

					System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
				}
			}

			// Step 6: (omitted in this example) Commit or rollback transaction
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
