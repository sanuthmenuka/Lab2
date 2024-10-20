import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Solution {
    public static void main(String[] args) {
        SenateBus bus = new SenateBus();
        Simulation simulation = new Simulation(bus);
        simulation.startSimulation();  // Start the simulation immediately
    }
}

class SenateBus {
    private static final int MAX_CAPACITY = 50;
    private int passengerCount = 0;
    private boolean isBoarding = false;
    private Queue<Passenger> waitingPassengers = new LinkedList<>();

    synchronized void boardBus(Passenger passenger) {
        while (!isBoarding || passengerCount >= MAX_CAPACITY) {
            try {
                wait();  // Wait if bus is not boarding or full
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        passengerCount++;
        System.out.println("Passenger boarded. Current count: " + passengerCount);

        if (passengerCount >= MAX_CAPACITY || waitingPassengers.isEmpty()) {
            notifyAll();  // Notify when bus is full or all passengers boarded
        }
    }

    synchronized void arrive() {
        System.out.println("Bus has arrived.");
        isBoarding = true;
        notifyAll();  // Notify waiting passengers to board

        while (passengerCount < MAX_CAPACITY && !waitingPassengers.isEmpty()) {
            try {
                wait();  // Wait until bus is full or no more passengers
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        depart();
    }

    synchronized void depart() {
        System.out.println("Bus departing with " + passengerCount + " passengers.");
        passengerCount = 0;  // Reset for the next bus
        isBoarding = false;
        notifyAll();  // Notify system that bus has departed
    }

    synchronized void addWaitingPassenger(Passenger passenger) {
        waitingPassengers.add(passenger);
    }

    synchronized boolean hasPassengers() {
        return !waitingPassengers.isEmpty();
    }
}

class Passenger implements Runnable {
    private SenateBus bus;

    Passenger(SenateBus bus) {
        this.bus = bus;
    }

    @Override
    public void run() {
        bus.addWaitingPassenger(this);  // Add passenger to the queue
        bus.boardBus(this);  // Try to board the bus
    }
}

class Simulation {
    private SenateBus bus;
    private Random random = new Random();
    private ExecutorService passengerPool = Executors.newFixedThreadPool(10); // Limit passenger threads

    Simulation(SenateBus bus) {
        this.bus = bus;
    }

    private double getExponential(double mean) {
        return -mean * Math.log(1 - random.nextDouble());
    }

    void startSimulation() {
        // Trigger first bus arrival immediately
        new Thread(() -> {
            bus.arrive();  // Bus arrives instantly
            while (true) {
                try {
                    Thread.sleep((long) (getExponential(20) * 60000));  // Bus arrives every 20 minutes on average
                    bus.arrive();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        // Trigger first passenger arrival immediately
        new Thread(() -> {
            passengerPool.submit(new Passenger(bus));  // First passenger arrives instantly
            while (true) {
                try {
                    Thread.sleep((long) (getExponential(0.5) * 1000));  // Passenger arrives every 30 seconds on average
                    Passenger passenger = new Passenger(bus);
                    passengerPool.submit(passenger);  // Use thread pool to handle passengers
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
