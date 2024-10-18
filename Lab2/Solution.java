import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Solution {
    public static void main(String[] args) {
        SenateBus bus = new SenateBus();
        Simulation simulation = new Simulation(bus);
        simulation.startSimulation();
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
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        passengerCount++;
        System.out.println("Passenger boarded. Current count: " + passengerCount);

        if (passengerCount >= MAX_CAPACITY || waitingPassengers.isEmpty()) {
            notifyAll();
        }
    }

    synchronized void arrive() {
        System.out.println("Bus has arrived.");
        isBoarding = true;
        notifyAll();

        while (passengerCount < MAX_CAPACITY && !waitingPassengers.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        depart();
    }

    synchronized void depart() {
        System.out.println("Bus departing with " + passengerCount + " passengers.");
        passengerCount = 0;
        isBoarding = false;
        notifyAll();
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
        bus.addWaitingPassenger(this);
        bus.boardBus(this);
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
        // Bus arrival simulation
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep((long) (getExponential(20) * 60000)); // Bus arrives every 20 minutes on average
                    bus.arrive();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        // Passenger arrival simulation
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep((long) (getExponential(0.5) * 1000)); // Passenger arrives every 30 seconds on average
                    Passenger passenger = new Passenger(bus);
                    passengerPool.submit(passenger); // Use thread pool to handle passengers
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
