import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {

    public static AtomicInteger inQueue = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {

        File directory = new File(args[0]);
        File[] files = directory.listFiles();

        File orders = files[1];
        File products = files[0];

        int NUMBER_OF_THREADS = Integer.parseInt(args[1]);

        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        try {
            File OrderOut = new File("orders_out.txt");
            File OrderProductsOut = new File("order_products_out.txt");
            if (OrderOut.createNewFile() && OrderProductsOut.createNewFile()) {
                System.out.println("File created: " + OrderOut.getName());
                System.out.println("File created: " + OrderProductsOut.getName());
            } else {
                System.out.println("File already exists.");
                PrintWriter writer1 = new PrintWriter("orders_out.txt");
                writer1.print("");
                writer1.close();

                PrintWriter writer2 = new PrintWriter("order_products_out.txt");
                writer2.print("");
                writer2.close();
            }
            //construirea thread-urilor de nivel 1
            Thread[] t = new Thread[NUMBER_OF_THREADS];
            for (int i = 0; i < NUMBER_OF_THREADS ; ++i) {
                t[i] = new Thread( new MyThreadLevel1(i ,NUMBER_OF_THREADS , orders , executor , inQueue , OrderOut ,OrderProductsOut,products));
                t[i].start();
            }

            for (int i = 0; i < NUMBER_OF_THREADS; ++i) {
                try {
                    t[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }
}

class MyThreadLevel1 implements Runnable {

    File order;

    int id;
    int NUMBER_OF_THREADS;

    ExecutorService executor;

    File OrderOut;
    File OrderProductsOut;
    File products;
    MyThreadLevel1(int id , int NUMBER_OF_THREADS ,File orders , ExecutorService executor , AtomicInteger inQueue , File OrderOut, File OrderProductsOut, File products ) {
        this.order=orders;
        this.id = id;
        this.NUMBER_OF_THREADS = NUMBER_OF_THREADS;
        this.executor=executor;
        this.OrderOut=OrderOut;
        this.OrderProductsOut=OrderProductsOut;
        this.products=products;

    }
    //scrierea in fisier
    public void write(String text) throws IOException {
        FileWriter fw = new FileWriter("orders_out.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(text);
        bw.newLine();
        bw.close();
    }

    @Override
    public void run() {

        String command;
        int count=1;
        boolean check=false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(order));
            while((command = reader.readLine()) != null){
                    if(count % NUMBER_OF_THREADS == id) {
                        String[] split = command.split(",", 2);
                        int counter;
                        counter = Integer.parseInt(split[1]);

                        if (counter == 0) {
                            check = true;
                        }

                        while (counter != 0 && check == false) {
                            Tema2.inQueue.incrementAndGet();
                            //adaugarea unui nou task cu numar de produs asociat
                            executor.submit(new MyThreadLevel2(split[0], counter, OrderProductsOut, id, products, executor));
                            counter--;
                        }
                            //verificare comanda valabila
                        if (check == false) {
                            String s = command + "," + "shipped";
                            try {
                                write(s);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                check=false;
                count++;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



class MyThreadLevel2 implements Runnable {

    File OrderProductsOut;

    int id;
    int order_number;
    File products;
    String order_id;

    ExecutorService executor;

    MyThreadLevel2(String order_id , int order_number , File OrderProductsOut , int id , File products , ExecutorService executor ) {
        this.id = id;
        this.OrderProductsOut=OrderProductsOut;
        this.products=products;
        this.order_id=order_id;
        this.order_number=order_number;
        this.executor=executor;
    }
    //scriere in fisierul de out
    public void write(String text) throws IOException {
        FileWriter fw = new FileWriter("order_products_out.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(text);
        bw.newLine();
        bw.close();
    }

    @Override
    public void run() {
        String command;
        int count=1;

        String line;

        //cautare in fisierul de produse
        try {
            BufferedReader reader = new BufferedReader(new FileReader(products));
            while((line = reader.readLine()) != null){
                String[] split = line.split(",", 2);

                if(order_id.equals(split[0])){
                    if(count == order_number ){
                        String s =line + "," +"shipped";
                        try {
                            write(s);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    count++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Tema2.inQueue.decrementAndGet();

        //verificare stare executor
        int left = Tema2.inQueue.get();
        if (left == 0) {
            executor.shutdown();
        }
    }
}