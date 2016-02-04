/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.store.format;

import java.util.function.Function;

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.kernel.impl.store.RecordStore;
import org.neo4j.kernel.impl.store.StoreHeader;
import org.neo4j.kernel.impl.store.record.AbstractBaseRecord;
import org.neo4j.kernel.impl.store.record.RecordLoad;

/**
 * Implementation of a very common type of format where the first byte, at least one bit in it,
 * say whether or not the record is in use. That can be used to let sub classes have simpler
 * read/write implementations.
 *
 * @param <RECORD> type of record.
 */
public abstract class BaseOneByteHeaderRecordFormat<RECORD extends AbstractBaseRecord> extends BaseRecordFormat<RECORD>
{
    protected BaseOneByteHeaderRecordFormat( Function<StoreHeader,Integer> recordSize, int recordHeaderSize,
            int inUseBitMaskForFirstByte )
    {
        super( recordSize, recordHeaderSize, inUseBitMaskForFirstByte );
    }

    @Override
    public final void read( RECORD record, PageCursor cursor, RecordLoad mode, int recordSize )
    {
        byte inUseByte = cursor.getByte();
        boolean inUse = isInUse( inUseByte );
        if ( mode.shouldLoad( inUse ) )
        {
            doRead( record, cursor, recordSize, inUseByte, inUse );
        }
    }

    /**
     * Reads contents at {@code cursor} into the given record. This method is only called if the {@link RecordLoad}
     * mode in {@link #read(AbstractBaseRecord, PageCursor, RecordLoad, int)} thinks it's OK to load the record,
     * given its inUse status.
     *
     * @param record to put read data into, replacing any existing data in that record object.
     * @param cursor {@link PageCursor} to read data from.
     * See {@link RecordStore#getRecord(long, AbstractBaseRecord, RecordLoad)} for more information.
     * @param recordSize size of records of this format. This is passed in like this since not all formats
     * know the record size in advance, but may be read from store header when opening the store.
     * @param inUseByte the first byte read, in order to determine inUse status.
     * @param inUse whether or not the record is in use. Keep in mind that this method may be called
     * even on an unused record, depending on {@link RecordLoad} mode.
     */
    protected abstract void doRead( RECORD record, PageCursor cursor, int recordSize, long inUseByte, boolean inUse );

    @Override
    public final void write( RECORD record, PageCursor cursor )
    {
        if ( record.inUse() )
        {
            doWrite( record, cursor );
        }
        else
        {
            byte inUseByte = cursor.getByte( cursor.getOffset() );
            inUseByte &= ~inUseBitMaskForFirstByte;
            cursor.putByte( inUseByte );
        }
    }

    /**
     * Writes record contents to the {@code cursor} in the format specified by this implementation if
     * the record is {@link AbstractBaseRecord#inUse()}, otherwise only the inUse bit is cleared and the
     * rest of the bytes in the record left untouched.
     *
     * @param record containing data to write.
     * @param cursor {@link PageCursor} to write the record data into.
     */
    protected abstract void doWrite( RECORD record, PageCursor cursor );
}