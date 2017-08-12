
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.InternalObservableUtils;

public class TT {
	public static void main(String[] args) throws Exception {
		// testMap();
		// testFlatMap();
		window();
	}
	
	

	public static void window() throws Exception {

		Observable.interval(1, TimeUnit.SECONDS).take(100).window(1, TimeUnit.SECONDS)
				.subscribe(new Observer<Observable<Long>>() {
					@Override
					public void onCompleted() {
						System.out.println("------>onCompleted()");
					}

					@Override
					public void onError(Throwable e) {
						System.out.println("------>onError()" + e);
					}

					@Override
					public void onNext(Observable<Long> integerObservable) {
						System.out.println("------->onNext()");
						integerObservable.subscribe(new Action1<Long>() {
							@Override
							public void call(Long integer) {
								System.out.println("------>call():" + integer);
							}
						});
					}
				});
		
		Thread.sleep(1000000);
	}

	public static void test() throws Exception {
		Observable<Integer> source = Observable.interval(50, TimeUnit.MILLISECONDS).map(i -> RandomUtils.nextInt(2));
		source.window(1, TimeUnit.SECONDS).subscribe(window -> {
			int[] metrics = new int[2];
			window.subscribe(i -> metrics[i]++, InternalObservableUtils.ERROR_NOT_IMPLEMENTED,
					() -> System.out.println("´°¿ÚMetrics:" + Arrays.toString(metrics)));
		});

		TimeUnit.SECONDS.sleep(30);
	}

	private static void testMap() throws Exception {

		String[] items = { "just1", "just2", "just3", "just4", "just5", "just6" };

		Observable<String> myObservable = Observable.from(items).map(new Func1<String, String>() {
			@Override
			public String call(String s) {
				return s;
			}
		});
		Subscriber<String> mySubscriber = new Subscriber<String>() {
			@Override
			public void onNext(String s) {
				System.out.println("onNext................." + s);
			}

			@Override
			public void onCompleted() {
				System.out.println("onCompleted.................");
			}

			@Override
			public void onError(Throwable e) {
				System.out.println("onError....................");
			}
		};
		myObservable.subscribe(mySubscriber);

	}

	private static void testFlatMap() {
		Integer[] items = { 1, 2, 3, 4, 5, 6 };

		Observable<String> myObservable = Observable.from(items).flatMap(new Func1<Integer, Observable<String>>() {
			@Override
			public Observable<String> call(Integer i) {
				return Observable.just("" + i);
			}
		});

		Subscriber<String> mySubscriber = new Subscriber<String>() {
			@Override
			public void onNext(String s) {
				System.out.println("onNext................." + s);
			}

			@Override
			public void onCompleted() {
				System.out.println("onCompleted.................");
			}

			@Override
			public void onError(Throwable e) {
				System.out.println("onError....................");
			}
		};

		myObservable.subscribe(mySubscriber);
	}

	private String flatMapInfo(Integer i) {
		return "flatmap" + i;
	}

}
