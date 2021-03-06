/*************************************************************************
 * Copyright (C) 2012 Philippe Leipold
 *
 * This file is part of BinaryFileDB.
 *
 * BinaryFileDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BinaryFileDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BinaryFileDB. If not, see <http://www.gnu.org/licenses/>.
 *
 **************************************************************************/

package de.Lathanael.BinaryFileDB.API;

import java.io.IOException;
import java.util.Map;

import de.Lathanael.BinaryFileDB.BaseClass.DataWriteQueue;
import de.Lathanael.BinaryFileDB.BaseClass.RecordsFile;
import de.Lathanael.BinaryFileDB.Exception.CacheSizeException;
import de.Lathanael.BinaryFileDB.Exception.QueueException;
import de.Lathanael.BinaryFileDB.Exception.RecordsFileException;
import de.Lathanael.BinaryFileDB.bukkit.Main;
import de.Lathanael.BinaryFileDB.bukkit.Metrics.Plotter;

/**
 * This class provides an API to a BinaryFile-DataBase through</br>
 * the use of a {@link de.Lathanael.BinaryFileDB.BaseClass.RecordsFile RecordsFile}.</br>
 * It also provides some methods to handle multiple files of the</br>
 * same type and a WriteQueue to lesser the I/O done by the</br>
 * file-database.
 * @author Lathanael (aka Philippe Leipold)
 */
public class DBAccess {

	private final RecordsFile file;
	private long TIMESTAMP;
	private boolean useQueue;
	private DataWriteQueue queue;
	private final int cacheSize;
	private int initialSize = 16;
	private int MAX_KEY_LENGTH = 64, DATA_START_HEADER_LOCATION = 4, RECORD_HEADER_LENGTH = 16, FILE_HEADERS_REGION_LENGTH = 16;
	private int reads = 0, writes = 0;
	private boolean custom = false;
	private Plotter plotter1, plotter2, plotter3;

	/**
	 * Creates a new database file. The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase efficiency.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess (String dbPath, int initialSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		cacheSize = 10;
		this.initialSize = initialSize;
		file = new RecordsFile(dbPath, initialSize, cacheSize);
		setMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, String accessFlags) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		cacheSize = 10;
		file = new RecordsFile(dbPath, accessFlags, cacheSize);
		setMetrics();
	}

	/**
	 * Creates a new database file. The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase efficiency.
	 * @param path - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String path, int initialSize, boolean useQueue, int queueSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		cacheSize = 10;
		this.initialSize = initialSize;
		file = new RecordsFile(path, initialSize, cacheSize);
		setMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index.
	 * @param path - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String path, String accessFlags, boolean useQueue, int queueSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		cacheSize = 10;
		file = new RecordsFile(path, accessFlags, cacheSize);
		setMetrics();
	}

	/**
	 * Creates a new database file.	The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase</br>
	 * efficiency. Let's the user define the length of keys etc.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, int initialSize, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		cacheSize = 10;
		this.initialSize = initialSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, initialSize, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Creates a new database file.	The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase</br>
	 * efficiency. Let's the user define the length of keys etc.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, int initialSize, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, boolean useQueue, int queueSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		cacheSize = 10;
		this.initialSize = initialSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, initialSize, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index</br>
	 * and let's the user define the length of keys etc.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, String accessFlags, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		cacheSize = 10;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, accessFlags, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index</br>
	 * and let's the user define the length of keys etc.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, String accessFlags, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, boolean useQueue, int queueSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		cacheSize = 10;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, accessFlags, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Creates a new database file.	The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase efficiency.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, int initialSize, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		this.cacheSize = cacheSize;
		this.initialSize = initialSize;
		file = new RecordsFile(dbPath, initialSize, cacheSize);
		setMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index.</br>
	 * The queue function is disabled.
	 * @param path - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String path, String accessFlags, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		this.cacheSize = cacheSize;
		file = new RecordsFile(path, accessFlags, cacheSize);
		setMetrics();
	}

	/**
	 * Creates a new database file. The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase efficiency.
	 * @param path - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String path, int initialSize, boolean useQueue, int queueSize, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		this.cacheSize = cacheSize;
		this.initialSize = initialSize;
		file = new RecordsFile(path, initialSize, cacheSize);
		setMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index.
	 * @param path - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String path, String accessFlags, boolean useQueue, int queueSize, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		this.cacheSize = cacheSize;
		file = new RecordsFile(path, accessFlags, cacheSize);
		setMetrics();
	}

	/**
	 * Creates a new database file.	The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase</br>
	 * efficiency. Let's the user define the length of keys etc.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, int initialSize, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		this.cacheSize = cacheSize;
		this.initialSize = initialSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, initialSize, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Creates a new database file.	The initialSize parameter determines the</br>
	 * amount of space which is allocated for the index. The index can grow</br>
	 * dynamically, but the parameter is provided to increase</br>
	 * efficiency. Let's the user define the length of keys etc.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param initialSize - Size of the db created
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, int initialSize, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, boolean useQueue, int queueSize,
			int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		this.cacheSize = cacheSize;
		this.initialSize = initialSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, initialSize, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index</br>
	 * and let's the user define the length of keys etc.</br>
	 * The queue function is disabled.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, String accessFlags, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		useQueue = false;
		this.cacheSize = cacheSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, accessFlags, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * Opens an existing database and initializes the in-memory index</br>
	 * and let's the user define the length of keys etc.
	 * @param dbPath - Pathname where the file is located as a String
	 * @param accessFlags - Whether the new DBFile should hava read-only "r" or read/write "rw" access
	 * @param MAX_KEY_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#MAX_KEY_LENGTH MAX_KEY_LENGTH}
	 * @param DATA_START_HEADER_LOCATION - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#DATA_START_HEADER_LOCATION DATA_START_HEADER_LOCATION}
	 * @param FILE_HEADERS_REGION_LENGTH - {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#FILE_HEADERS_REGION_LENGTH FILE_HEADERS_REGION_LENGTH}
	 * @param RECORD_HEADER_LENGTH {@link de.Lathanael.BinaryFileDB.BaseClass.BaseRecordsFile#RECORD_HEADER_LENGTH RECORD_HEADER_LENGTH}
	 * @param useQueue - If set to true a queue will be created to lessen IO stress on the Disk
	 * @param queueSize - Size of the queue if it is used it must be greater 0!
	 * @param cacheSize - Size of the initial cache for recently loaded RecordReaders. Must be greater 0!
	 * @throws CacheSizeException
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public DBAccess(String dbPath, String accessFlags, int MAX_KEY_LENGTH, int DATA_START_HEADER_LOCATION,
			int FILE_HEADERS_REGION_LENGTH, int RECORD_HEADER_LENGTH, boolean useQueue, int queueSize,
			int cacheSize) throws IOException, RecordsFileException, CacheSizeException {
		TIMESTAMP = System.currentTimeMillis();
		this.useQueue = useQueue;
		if (useQueue)
			queue = new DataWriteQueue(queueSize);
		this.cacheSize = cacheSize;
		this.MAX_KEY_LENGTH = MAX_KEY_LENGTH;
		this.DATA_START_HEADER_LOCATION = DATA_START_HEADER_LOCATION;
		this.FILE_HEADERS_REGION_LENGTH = FILE_HEADERS_REGION_LENGTH;
		this.RECORD_HEADER_LENGTH = RECORD_HEADER_LENGTH;
		file = new RecordsFile(dbPath, accessFlags, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
		setCustomMetrics();
	}

	/**
	 * This will load and return a new DBAccess instance with standard settings.
	 * @param path - Path to the file
	 * @param accessFlags - read only(r) or read and write(rw) access
	 * @return A new {@link de.Lathanael.BinaryFileDB.API.DBAccess DBAccess} instance
	 * @throws IOException
	 * @throws RecordsFileException
	 * @throws CacheSizeException
	 */
	public DBAccess loadNewDB(String path, String accessFlags) throws IOException, RecordsFileException, CacheSizeException {
		return new DBAccess(path, accessFlags);
	}

	/**
	 * Creates a new DBAccess instance with standard settings.
	 * @param path - Path to the file
	 * @param accessFlags  - read only(r) or read and write(rw) access
	 * @return A new {@link de.Lathanael.BinaryFileDB.API.DBAccess DBAccess} instance
	 * @throws IOException
	 * @throws RecordsFileException
	 * @throws CacheSizeException
	 */
	public DBAccess createNewDB(String path, String accessFlags) throws IOException, RecordsFileException, CacheSizeException {
		return new DBAccess(path, initialSize, cacheSize);
	}

	/**
	 * This will load and return a new DBAccess instance with custom</br>
	 * settings specified during creation of this DBAccess instance.
	 * @param path - Path to the file
	 * @param accessFlags - read only(r) or read and write(rw) access
	 * @return A new {@link de.Lathanael.BinaryFileDB.API.DBAccess DBAccess} instance
	 * @throws IOException
	 * @throws RecordsFileException
	 * @throws CacheSizeException
	 */
	public DBAccess loadNewCustomDB(String path, String accessFlags) throws IOException, RecordsFileException, CacheSizeException {
		return new DBAccess(path, accessFlags, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
	}

	/**
	 * Creates a new DBAccess instance with the parameteres</br>
	 * given at the creation of this instance.
	 * @param path - Path to the file
	 * @param accessFlags  - read only(r) or read and write(rw) access
	 * @return A new {@link de.Lathanael.BinaryFileDB.API.DBAccess DBAccess} instance
	 * @throws IOException
	 * @throws RecordsFileException
	 * @throws CacheSizeException
	 */
	public DBAccess createNewCustomDB(String path, String accessFlags) throws IOException, RecordsFileException, CacheSizeException {
		return new DBAccess(path, initialSize, cacheSize, MAX_KEY_LENGTH, DATA_START_HEADER_LOCATION, FILE_HEADERS_REGION_LENGTH, RECORD_HEADER_LENGTH);
	}

	private void setMetrics() {
		Main.addToAccess();
		Main.graph.addPlotter(plotter1 = new Plotter() {

			@Override
			public int getValue() {
				return reads;
			}

			@Override
			public String getColumnName() {
				return "Total reads";
			}
		});
		Main.graph.addPlotter(plotter2 = new Plotter() {

			@Override
			public int getValue() {
				return writes;
			}

			@Override
			public String getColumnName() {
				return "Total writes";
			}
		});
		Main.graph.addPlotter(plotter3 = new Plotter() {

			@Override
			public int getValue() {
				return file.getNumRecords();
			}

			@Override
			public String getColumnName() {
				return "Total Records";
			}
		});
	}

	private void setCustomMetrics() {
		custom = true;
		Main.addToCustomAccess();
		Main.graph.addPlotter(plotter1 = new Plotter() {

			@Override
			public int getValue() {
				return reads;
			}

			@Override
			public String getColumnName() {
				return "Total reads";
			}
		});
		Main.graph.addPlotter(plotter2 = new Plotter() {

			@Override
			public int getValue() {
				return writes;
			}

			@Override
			public String getColumnName() {
				return "Total writes";
			}
		});
		Main.graph.addPlotter(plotter3 = new Plotter() {

			@Override
			public int getValue() {
				return file.getNumRecords();
			}

			@Override
			public String getColumnName() {
				return "Total Records";
			}
		});
	}

	/**
	 * Get the RecordsFile associated with this instance.
	 */
	public RecordsFile getRecordsFile() {
		return file;
	}

	/**
	 * Activates the queue function.
	 */
	public void activateQueue() {
		useQueue = true;
	}

	/**
	 * Deactivates the queue function.
	 */
	public void deactivateQueue() {
		useQueue = false;
	}

	/**
	 * Gets a record as a {@link de.Lathanael.BinaryFileDB.API.RecordReader RecordReader} object from the DB.
	 * </p>
	 * If the WriteQueue is activated it will first check if the entry is scheduled and get the changed Record.
	 * @param key
	 * @return RecordReader object associated to the key or {@code null}
	 * @throws RecordsFileException
	 * @throws IOException
	 */
	public RecordReader getRecord(String key) throws RecordsFileException, IOException {
		reads++;
		RecordWriter rw;
		if (useQueue &&  (rw = queue.getQueuedItem(key)) != null)
			return new RecordReader(key, rw);
		RecordReader rr = null;
		try {
			rr = file.readRecord(key);
		} catch (RecordsFileException e) {
			//Only Key not found possible here!
			return null;
		}
		return rr;
	}

	/**
	 * Writes a Record to the DB. Existing ones will be updated.
	 *  </p>
	 * If the queue is activated it will try to add the RecordWriter to it.</br>
	 * If it fails it will write the whole queue to the DB, clear it and finally</br>
	 * add the given RecordWriter to the now empty queue.
	 * @param rw - RecordWriter object which should be written
	 * @throws RecordsFileException
	 * @throws IOException
	 * @throws QueueException
	 */
	public void writeRecord(RecordWriter rw) throws RecordsFileException, IOException, QueueException {
		writes++;
		if (useQueue) {
			if(!queue.addToQueue(rw)) {
				for (Map.Entry<String, RecordWriter> entry: queue.getQueue().entrySet()) {
					boolean append = entry.getValue().isAppend();
					if (file.recordExists(entry.getValue().getKey())) {
						file.updateRecord(entry.getValue());
						continue;
					}
					if (append) {
						file.quickInsertRecord(entry.getValue());
						continue;
					}
					file.insertRecord(entry.getValue());
				}
				queue.clearQueue();
				if (!queue.addToQueue(rw))
					throw new QueueException("Could not add Item to the queue");
			}
		} else {
			if (file.recordExists(rw.getKey()))
				file.updateRecord(rw);
			else if (rw.isAppend())
				file.quickInsertRecord(rw);
			else
				file.insertRecord(rw);
		}
		file.removeFromCache(rw.getKey());
		updateTimeStamp();
	}

	/**
	 * Writes a Record to the DB without searching for free-space. Existing ones will be updated.
	 * </p>
	 * If the queue is activated it will try to add the RecordWriter to it.</br>
	 * If it fails it will write the whole queue to the DB, clear it and finally</br>
	 * add the given RecordWriter to the now empty queue.
	 * @param rw - RecordWriter object which should be written
	 * @throws IOException
	 * @throws RecordsFileException
	 * @throws QueueException
	 */
	public void appendRecord(RecordWriter rw) throws RecordsFileException, IOException, QueueException {
		writes++;
		rw.setAppend(true);
		if (useQueue) {
			if(!queue.addToQueue(rw)) {
				for (Map.Entry<String, RecordWriter> entry: queue.getQueue().entrySet()) {
					if (file.recordExists(entry.getValue().getKey())) {
						file.updateRecord(entry.getValue());
						continue;
					}
					file.quickInsertRecord(entry.getValue());
				}
				queue.clearQueue();
				if (!queue.addToQueue(rw))
					throw new QueueException("Could not add Item to the queue");
			}
		} else {
			if (file.recordExists(rw.getKey()))
				file.updateRecord(rw);
			else
				file.quickInsertRecord(rw);
		}
		file.removeFromCache(rw.getKey());
		updateTimeStamp();
	}

	/**
	 * Writes a Record to the DB ignoring the queue. Existing ones will be updated.
	 * @param rw - RecordWriter object which should be written
	 * @throws RecordsFileException
	 * @throws IOException
	 */
	public void quickWriteRecord(RecordWriter rw) throws RecordsFileException, IOException {
		writes++;
		if (file.recordExists(rw.getKey()))
			file.updateRecord(rw);
		else
			file.insertRecord(rw);
		file.removeFromCache(rw.getKey());
		updateTimeStamp();
	}

	/**
	 * Writes a Record to the DB without searching for free-space and ignoring the queue.</br>
	 * Existing ones will be updated.
	 * @param rw - RecordWriter object which should be written
	 * @throws RecordsFileException
	 * @throws IOException
	 */
	public void quickInsertRecord(RecordWriter rw) throws RecordsFileException, IOException {
		writes++;
		if (file.recordExists(rw.getKey()))
			file.updateRecord(rw);
		else
			file.quickInsertRecord(rw);
		file.removeFromCache(rw.getKey());
		updateTimeStamp();
	}

	/**
	 * Deletes the Record associated with the key. This will also remove the record </br>
	 * from the queue if it is activated and the record is in it.
	 * @param key - The key as a String to indicate which Record should be deleted
	 * @throws RecordsFileException
	 * @throws IOException
	 */
	public void deleteRecord(String key) throws RecordsFileException, IOException {
		writes++;
		if (useQueue)
			queue.removeQueuedItem(key);
		file.deleteRecord(key);
		file.removeFromCache(key);
		updateTimeStamp();
	}

	/**
	 * Gets the TIMESTAMP when this instance was created or last modified.
	 */
	public long getTimeStamp() {
		return TIMESTAMP;
	}

	/**
	 * Sets the TIMESTAMP to the current System-time, indicating the file was changed.
	 */
	private void updateTimeStamp() {
		TIMESTAMP = System.currentTimeMillis();
	}

	/**
	 * Closes this instance of the DataBase.
	 * @param unsafe - If true the file will be closed without saving pending records
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	public void closeDB(boolean unsafe) throws IOException, RecordsFileException {
		if (custom)
			Main.removeFromCustomAccess();
		else
			Main.removeFromAccess();
		try {
			removePlotters();
		} catch (Exception e) {
		}
		if (unsafe)
			forceCloseDB();
		else
			safelyCloseDB();
	}

	/**
	 * Close the database safley. This will save all things in the queue before closing the file.
	 * @throws RecordsFileException
	 * @throws IOException
	 */
	private void safelyCloseDB() throws IOException, RecordsFileException {
		if (useQueue) {
			for (Map.Entry<String, RecordWriter> entry: queue.getQueue().entrySet()) {
				writes++;
				if (file.recordExists(entry.getValue().getKey())) {
					file.updateRecord(entry.getValue());
					continue;
				}
				file.insertRecord(entry.getValue());
			}
		}
		file.close();
	}

	/**
	 * Close the DB without saving things in the queue
	 * @throws IOException
	 * @throws RecordsFileException
	 */
	private void forceCloseDB() throws IOException, RecordsFileException {
		file.close();
	}

	private void removePlotters() {
		Main.graph.removePlotter(plotter1);
		Main.graph.removePlotter(plotter2);
		Main.graph.removePlotter(plotter3);
	}
}
