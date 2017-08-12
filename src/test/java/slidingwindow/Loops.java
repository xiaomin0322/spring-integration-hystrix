package slidingwindow;  
  
import java.util.concurrent.TimeUnit;  
  
/** 
 * Created by admin on 2016/02/20. 
 */  
public class Loops {  
  
    public static void dieLoop(Runnable runnable) {  
        while (true) {  
            run(runnable);  
        }  
    }  
  
    public static void rateLoop(Runnable runnable, int mills) {  
        while (true) {  
            try {  
                TimeUnit.MILLISECONDS.sleep(mills);  
            } catch (InterruptedException e) {  
  
            }  
            run(runnable);  
        }  
    }  
  
    public static void fixLoop(Runnable runnable, int loop) {  
        for (int i = 0; i < loop; i++) {  
            run(runnable);  
        }  
    }  
  
    private static void run(Runnable runnable) {  
        try {  
            runnable.run();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
  
}  