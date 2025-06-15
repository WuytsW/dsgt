package hotel;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

public class BookingManager implements BookingManagerInterface {

	private Room[] rooms;

	public BookingManager() {
		this.rooms = initializeRooms();
	}

	public Set<Integer> getAllRooms() throws RemoteException {
		try {
			Set<Integer> allRooms = new HashSet<Integer>();
			Iterable<Room> roomIterator = Arrays.asList(rooms);
			for (Room room : roomIterator) {
				allRooms.add(room.getRoomNumber());
			}
			return allRooms;
		} catch (Exception e) {
			throw new RemoteException("Error retrieving all rooms", e);
		}
	}

	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) throws RemoteException {
		try {
			for (Room room : this.rooms) {
				if (room.getRoomNumber().equals(roomNumber)) {
					for (BookingDetail booking : room.getBookings()) {
						if (booking.getDate().isEqual(date)) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			throw new RemoteException("Error checking room availability", e);
		}
	}

	public void addBooking(BookingDetail bookingDetail) throws RemoteException {
		try {
			for (Room room : this.rooms) {
				if (room.getRoomNumber().equals(bookingDetail.getRoomNumber())) {
					room.getBookings().add(bookingDetail);
				}
			}
		} catch (Exception e) {
			throw new RemoteException("Error adding booking", e);
		}
	}

	public Set<Integer> getAvailableRooms(LocalDate date) throws RemoteException {
		try {
			Set<Integer> output = new HashSet<Integer>();
			for (Room room : this.rooms) {
				boolean isAvailable = true;
				for (BookingDetail booking : room.getBookings()) {
					if (booking.getDate().isEqual(date)) {
						isAvailable = false;
						break;
					}
				}
				if (isAvailable) {
					output.add(room.getRoomNumber());
				}
			}
			return output;
		} catch (Exception e) {
			throw new RemoteException("Error retrieving available rooms", e);
		}
	}

	private static Room[] initializeRooms() {
		Room[] rooms = new Room[4];
		rooms[0] = new Room(101);
		rooms[1] = new Room(102);
		rooms[2] = new Room(201);
		rooms[3] = new Room(203);
		return rooms;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			System.setProperty("java.rmi.server.hostname", args[0]);
		}
		try {
			// Create server instance
			BookingManager bookingManager = new BookingManager();

			// Export remote object and bind to registry
			BookingManagerInterface stub = (BookingManagerInterface) UnicastRemoteObject.exportObject(bookingManager, 0);

			Registry registry = LocateRegistry.createRegistry(1099);
			registry.rebind("BookingService", stub);

			System.out.println("Server ready");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
