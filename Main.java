import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;

import javax.swing.*;
import java.util.*;

class Order {
    enum Type { BUY, SELL }
    private static int idCounter = 1;

    int orderId;
    Type type;
    double price;
    int quantity;
    long timestamp;

    public Order(Type type, double price, int quantity) {
        this.orderId = idCounter++;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.nanoTime();
    }
}

class OrderBook {
    PriorityQueue<Order> buyOrders;
    PriorityQueue<Order> sellOrders;
    List<double[]> tradeData = new ArrayList<>();

    public OrderBook() {
        // Buy orders sorted by highest price
        buyOrders = new PriorityQueue<>((o1, o2) -> Double.compare(o2.price, o1.price));

        // Sell orders sorted by lowest price
        sellOrders = new PriorityQueue<>(Comparator.comparingDouble(o -> o.price));
    }

    public void placeOrder(Order order) {
        if (order.type == Order.Type.BUY) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }
        matchOrders();
    }

    private void matchOrders() {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            Order buy = buyOrders.peek();
            Order sell = sellOrders.peek();

            if (buy.price >= sell.price) {
                int executedQuantity = Math.min(buy.quantity, sell.quantity);
                System.out.println("Trade Executed: " + executedQuantity + " units at $" + sell.price);

                // Store trade data
                tradeData.add(new double[]{System.currentTimeMillis(), buy.price, sell.price});

                buy.quantity -= executedQuantity;
                sell.quantity -= executedQuantity;

                if (buy.quantity == 0) buyOrders.poll();
                if (sell.quantity == 0) sellOrders.poll();
            } else {
                break;
            }
        }
    }

    public List<double[]> getTradeData() {
        return tradeData;
    }
}

class CandlestickChart extends JFrame {
    public CandlestickChart(String title, List<double[]> tradeData) {
        super(title);
        DefaultHighLowDataset dataset = createDataset(tradeData);
        JFreeChart chart = ChartFactory.createCandlestickChart(
                "Stock Market Trading",
                "Time",
                "Price",
                dataset,
                false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(new CandlestickRenderer());
        plot.setDomainAxis(new DateAxis("Time"));

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(panel);
    }

    private DefaultHighLowDataset createDataset(List<double[]> tradeData) {
        int size = tradeData.size();
        Date[] date = new Date[size];
        double[] high = new double[size];
        double[] low = new double[size];
        double[] open = new double[size];
        double[] close = new double[size];
        double[] volume = new double[size];

        for (int i = 0; i < size; i++) {
            date[i] = new Date((long) tradeData.get(i)[0]);
            open[i] = tradeData.get(i)[1];
            close[i] = tradeData.get(i)[2];
            high[i] = Math.max(open[i], close[i]) + 2; // Adding buffer for visualization
            low[i] = Math.min(open[i], close[i]) - 2;
            volume[i] = 1000; // Dummy volume
        }
        return new DefaultHighLowDataset("Candlestick Chart", date, high, low, open, close, volume);
    }
}

public class Main {
    static Random random = new Random();

    public static Order randomOrder() {
        Order.Type type = random.nextBoolean() ? Order.Type.BUY : Order.Type.SELL;
        double price = 100 + random.nextInt(20) + random.nextDouble(); // Price between $100-$120
        int quantity = 1 + random.nextInt(10); // Quantity between 1-10
        return new Order(type, price, quantity);
    }

    public static void main(String[] args) {
        OrderBook orderBook = new OrderBook();

        // Generate 1000 random buy/sell orders
        for (int i = 0; i < 1000; i++) {
            Order order = randomOrder();
            orderBook.placeOrder(order);
        }

        // Display the candlestick chart
        SwingUtilities.invokeLater(() -> {
            CandlestickChart chart = new CandlestickChart("Stock Market Trading", orderBook.getTradeData());
            chart.pack();
            chart.setVisible(true);
        });
    }
}
