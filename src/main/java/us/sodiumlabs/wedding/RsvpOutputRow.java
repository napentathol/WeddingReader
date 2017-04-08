package us.sodiumlabs.wedding;

import com.google.common.base.MoreObjects;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * @author Alex on 4/8/2017.
 */
public class RsvpOutputRow {
    public final String firstName;
    public final String lastName;
    public final String attendance;
    public final String qcc;
    public final boolean primary;

    public RsvpOutputRow( String firstName, String lastName, String attendance, String qcc, boolean primary ) {
        this.firstName = requireNonNull( firstName );
        this.lastName = requireNonNull( lastName );
        this.attendance = requireNonNull( attendance );
        this.qcc = qcc;
        this.primary = primary;
    }

    public void printRsvpOutputRow( final CSVPrinter printer ) throws IOException {
        printer.print( firstName );
        printer.print( lastName );
        printer.print( attendance );
        printer.print( primary );
        printer.print( qcc );
        printer.println();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper( this.getClass() )
            .add( "firstName", firstName )
            .add( "lastName", lastName )
            .add( "attendance", attendance )
            .add( "qcc", qcc )
            .add( "primary", primary )
            .toString();
    }
}
