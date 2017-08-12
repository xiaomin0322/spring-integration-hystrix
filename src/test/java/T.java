

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.functions.Action1;

public class T {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		  AtomicLong atomicLong = new AtomicLong(0);
	       
		  
		  
		  Observable.interval(0, 10, TimeUnit.SECONDS)
           .subscribe(new Action1<Long>() {
               @Override
               public void call(Long aLong) {
                   System.err.println("call:"+atomicLong.get());
               }
           });
	        
	        
	        new Thread(){
	        	public void run() {
	        		for(;;){
	        			System.out.println("µ±Ç°£º"+atomicLong.incrementAndGet());
	        			try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
	        		}
	        	}
	        }.start();
	        
	}

}
