package rw2018.statistics.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import rw2018.statistics.StatisticsDB;
import rw2018.statistics.TriplePosition;
import rw2018.statistics.utilities.NumberConversion;

/**
 * This is the class that will be executed during the evaluation!!
 * 
 * TODO implement this class
 */
public class StatisticsDBImpl implements StatisticsDB {

	private static int MAX_BYTES = 2;
	
	private int numberOfChunks;

	private File statisticsFile;

	private RandomAccessFile statistics;

	@Override
	public void setUp(File statisticsDir, int numberOfChunks) {
		this.numberOfChunks = numberOfChunks;
		if (!statisticsDir.exists()) {
			statisticsDir.mkdirs();
		}
		statisticsFile = new File(statisticsDir.getAbsolutePath() + File.separator + "statistics");
		try {
			statistics = new RandomAccessFile(statisticsFile, "rw");
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getNumberOfChunks() {
		return numberOfChunks;
	}

	@Override
	public void incrementFrequency(long resourceId, int chunkNumber, TriplePosition triplePosition) {
		if (resourceId <= 0) {
			throw new IllegalArgumentException("Illegal resource Id " + resourceId + ". Resource ids must be > 0.");
		}
		try {
			long sizeOfRow = MAX_BYTES * numberOfChunks * getTriplePositions().length;
			int indexOfTriplePosition = -1;
			for (int i = 0; i < getTriplePositions().length; i++) {
				if (getTriplePositions()[i] == triplePosition) {
					indexOfTriplePosition = i;
				}
			}
			if (indexOfTriplePosition == -1) {
				throw new IllegalArgumentException(
						"The triple position " + triplePosition + " is not supported. Supported triple positions are "
								+ Arrays.toString(getTriplePositions()) + ".");
			}
			long offset = ((resourceId - 1) * sizeOfRow)
					+ (((indexOfTriplePosition * numberOfChunks) + chunkNumber) * MAX_BYTES);

			statistics.seek(offset);
			int value = 1;
			try {
				value = statistics.read(new byte[MAX_BYTES]);
				value++;
			} catch (EOFException e) {
				// the resource did not exist in the file yet
			}
			if (statistics.getFilePointer() != offset) {
				statistics.seek(offset);
			}
			statistics.write(new byte[] {
					(byte)(value & 0xFF), 
					(byte)((value >>> 8) & 0xFF), 
					(byte)((value >>> 16) & 0xFF)});
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getFrequency(long resourceId, int chunkNumber, TriplePosition triplePosition) {
		if (resourceId <= 0) {
			return -1;
		}
		try {
			long sizeOfRow = MAX_BYTES * numberOfChunks * getTriplePositions().length;
			int indexOfTriplePosition = -1;
			int columnNumber = 0;
			for (int i = 0; i < getTriplePositions().length; i++) {
				if (getTriplePositions()[i] == triplePosition) {
					indexOfTriplePosition = i;
					break;
				} else {
					columnNumber += numberOfChunks;
				}
			}
			if (indexOfTriplePosition == -1) {
				throw new IllegalArgumentException(
						"The triple position " + triplePosition + " is not supported. Supported triple positions are "
								+ Arrays.toString(getTriplePositions()) + ".");
			}
			long offset = ((resourceId - 1) * sizeOfRow) + ((columnNumber + chunkNumber) * MAX_BYTES);

			statistics.seek(offset);
			try {
				return statistics.read(new byte[MAX_BYTES]);
			} catch (EOFException e) {
				// the resource did not exist in the file yet
				if (statistics.length() >= ((resourceId - 1) * sizeOfRow)) {
					// the resource exists but does not have a value for the current
					// element
					return 0;
				} else {
					// the resource does not exist
					return -1;
				}
			}

		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	@Override
	public long[] getFrequencies(long resourceId) {
		if (resourceId <= 0) {
			return null;
		}
		try {
			int sizeOfRow = MAX_BYTES * numberOfChunks * getTriplePositions().length;
			long offset = (resourceId - 1) * sizeOfRow;

			byte[] row = new byte[sizeOfRow];
			statistics.seek(offset);
			int readBytes = statistics.read(row);
			if (readBytes < 1) {
				return null;
			}

			long[] result = new long[row.length / MAX_BYTES];
			for (int i = 0; i < result.length; i++) {
				//result[i] = NumberConversion.bytes2long(row, i * MAX_BYTES);
				result[i] = NumberConversion.bytes2short(row, i * MAX_BYTES);
			}
			return result;
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			if (statistics != null) {
				statistics.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
