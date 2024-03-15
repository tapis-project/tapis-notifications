/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records;


import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.EventSeries;

import java.time.LocalDateTime;

import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Record7;
import org.jooq.Row7;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EventSeriesRecord extends UpdatableRecordImpl<EventSeriesRecord> implements Record7<String, String, String, String, Long, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>tapis_ntf.event_series.tenant</code>.
     */
    public void setTenant(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.tenant</code>.
     */
    public String getTenant() {
        return (String) get(0);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.source</code>.
     */
    public void setSource(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.source</code>.
     */
    public String getSource() {
        return (String) get(1);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.subject</code>.
     */
    public void setSubject(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.subject</code>.
     */
    public String getSubject() {
        return (String) get(2);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.series_id</code>.
     */
    public void setSeriesId(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.series_id</code>.
     */
    public String getSeriesId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.seq_count</code>.
     */
    public void setSeqCount(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.seq_count</code>.
     */
    public Long getSeqCount() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.created</code>.
     */
    public void setCreated(LocalDateTime value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(5);
    }

    /**
     * Setter for <code>tapis_ntf.event_series.updated</code>.
     */
    public void setUpdated(LocalDateTime value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_ntf.event_series.updated</code>.
     */
    public LocalDateTime getUpdated() {
        return (LocalDateTime) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record4<String, String, String, String> key() {
        return (Record4) super.key();
    }

    // -------------------------------------------------------------------------
    // Record7 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row7<String, String, String, String, Long, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    @Override
    public Row7<String, String, String, String, Long, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row7) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return EventSeries.EVENT_SERIES.TENANT;
    }

    @Override
    public Field<String> field2() {
        return EventSeries.EVENT_SERIES.SOURCE;
    }

    @Override
    public Field<String> field3() {
        return EventSeries.EVENT_SERIES.SUBJECT;
    }

    @Override
    public Field<String> field4() {
        return EventSeries.EVENT_SERIES.SERIES_ID;
    }

    @Override
    public Field<Long> field5() {
        return EventSeries.EVENT_SERIES.SEQ_COUNT;
    }

    @Override
    public Field<LocalDateTime> field6() {
        return EventSeries.EVENT_SERIES.CREATED;
    }

    @Override
    public Field<LocalDateTime> field7() {
        return EventSeries.EVENT_SERIES.UPDATED;
    }

    @Override
    public String component1() {
        return getTenant();
    }

    @Override
    public String component2() {
        return getSource();
    }

    @Override
    public String component3() {
        return getSubject();
    }

    @Override
    public String component4() {
        return getSeriesId();
    }

    @Override
    public Long component5() {
        return getSeqCount();
    }

    @Override
    public LocalDateTime component6() {
        return getCreated();
    }

    @Override
    public LocalDateTime component7() {
        return getUpdated();
    }

    @Override
    public String value1() {
        return getTenant();
    }

    @Override
    public String value2() {
        return getSource();
    }

    @Override
    public String value3() {
        return getSubject();
    }

    @Override
    public String value4() {
        return getSeriesId();
    }

    @Override
    public Long value5() {
        return getSeqCount();
    }

    @Override
    public LocalDateTime value6() {
        return getCreated();
    }

    @Override
    public LocalDateTime value7() {
        return getUpdated();
    }

    @Override
    public EventSeriesRecord value1(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public EventSeriesRecord value2(String value) {
        setSource(value);
        return this;
    }

    @Override
    public EventSeriesRecord value3(String value) {
        setSubject(value);
        return this;
    }

    @Override
    public EventSeriesRecord value4(String value) {
        setSeriesId(value);
        return this;
    }

    @Override
    public EventSeriesRecord value5(Long value) {
        setSeqCount(value);
        return this;
    }

    @Override
    public EventSeriesRecord value6(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public EventSeriesRecord value7(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public EventSeriesRecord values(String value1, String value2, String value3, String value4, Long value5, LocalDateTime value6, LocalDateTime value7) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached EventSeriesRecord
     */
    public EventSeriesRecord() {
        super(EventSeries.EVENT_SERIES);
    }

    /**
     * Create a detached, initialised EventSeriesRecord
     */
    public EventSeriesRecord(String tenant, String source, String subject, String seriesId, Long seqCount, LocalDateTime created, LocalDateTime updated) {
        super(EventSeries.EVENT_SERIES);

        setTenant(tenant);
        setSource(source);
        setSubject(subject);
        setSeriesId(seriesId);
        setSeqCount(seqCount);
        setCreated(created);
        setUpdated(updated);
    }
}
