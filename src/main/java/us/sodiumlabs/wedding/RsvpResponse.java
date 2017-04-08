package us.sodiumlabs.wedding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Alex on 4/7/2017.
 */
public class RsvpResponse {
    public final String firstName;
    public final String lastName;
    public final String attendance;
    public final List<Guest> guests;
    public final String qcc;

    @JsonCreator
    public RsvpResponse(
        @JsonProperty( "firstName" ) String firstName,
        @JsonProperty( "lastName" ) String lastName,
        @JsonProperty( "attendance" ) String attendance,
        @JsonProperty( "guests" ) List<Guest> guests,
        @JsonProperty( "qcc" ) String qcc
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.attendance = attendance;
        this.guests = guests;
        this.qcc = qcc;
    }

    public static class Guest {
        public final String firstName;
        public final String lastName;

        @JsonCreator
        public Guest(
            @JsonProperty( "firstName" ) String firstName,
            @JsonProperty( "lastName" ) String lastName
        ) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
