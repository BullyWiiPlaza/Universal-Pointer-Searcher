import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnsafeTesting
{
	private ByteBuffer destinationByteBuffer;
	private ByteBuffer sourceByteBuffer;

	private UnsafeTesting()
	{
		int capacity = 1_000_000_000;
		destinationByteBuffer = allocateByteBuffer(capacity);
		sourceByteBuffer = allocateByteBuffer(capacity);

		for (int byteBufferIndex = 0; byteBufferIndex < sourceByteBuffer.capacity() - 3; byteBufferIndex += 4)
		{
			sourceByteBuffer.putInt(byteBufferIndex);
		}

		destinationByteBuffer.clear();
		sourceByteBuffer.clear();
	}

	private ByteBuffer allocateByteBuffer(int capacity)
	{
		return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
	}

	private void runTest(boolean useUnsafeMethod) throws Exception
	{
		Unsafe unsafe = getUnsafeInstance();
		long destinationByteBufferAddress = ((DirectBuffer) destinationByteBuffer).address();
		long sourceByteBufferAddress = ((DirectBuffer) sourceByteBuffer).address();

		int executionsCount = 0;

		if (useUnsafeMethod)
		{
			for (int sourceBufferIndex = 0; sourceBufferIndex < destinationByteBuffer.remaining() - 3; sourceBufferIndex += 4)
			{
				long sourceOffset = sourceByteBufferAddress + sourceBufferIndex;
				int value = unsafe.getInt(sourceOffset);

				long targetOffset = destinationByteBufferAddress + sourceBufferIndex;
				unsafe.putInt(targetOffset, value);

				executionsCount++;
			}
		} else
		{
			while (sourceByteBuffer.remaining() > 3)
			{
				int value = destinationByteBuffer.getInt();
				sourceByteBuffer.putInt(value);

				executionsCount++;
			}
		}

		/*sourceByteBuffer.position(0);
		destinationByteBuffer.position(0);
		boolean equal = sourceByteBuffer.equals(destinationByteBuffer);

		if (!equal)
		{
			throw new IllegalStateException("Buffers not equal!");
		}*/

		System.out.println("Executions: " + executionsCount);
	}

	private static Unsafe getUnsafeInstance() throws Exception
	{
		Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
		unsafe.setAccessible(true);

		return (Unsafe) unsafe.get(null);
	}

	private static void runTest(UnsafeTesting unsafeTesting, boolean useUnsafeMethod) throws Exception
	{
		long startingTime = System.nanoTime();
		unsafeTesting.runTest(useUnsafeMethod);
		long nanoSecondsTaken = System.nanoTime() - startingTime;
		double milliSecondsTaken = nanoSecondsTaken / 1e6;
		System.out.println(milliSecondsTaken + " milliseconds taken");
	}

	public static void main(String[] arguments) throws Exception
	{
		UnsafeTesting unsafeTesting = new UnsafeTesting();

		System.out.println("### Unsafe ###");
		runTest(unsafeTesting, true);
		System.out.println();

		System.out.println("### Direct ###");
		runTest(unsafeTesting, false);
	}
}