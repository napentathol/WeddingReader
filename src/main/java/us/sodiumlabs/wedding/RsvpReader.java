package us.sodiumlabs.wedding;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Alex on 4/7/2017.
 */
public class RsvpReader {
    private final AmazonS3 amazonS3;

    private final String bucketName;

    private final ObjectMapper objectMapper;

    public RsvpReader( AmazonS3 amazonS3, String bucketName, ObjectMapper objectMapper ) {
        this.amazonS3 = requireNonNull( amazonS3 );
        this.bucketName = requireNonNull( bucketName );
        this.objectMapper = requireNonNull( objectMapper );
    }

    private String getRsvpFile( Request request, Response response ) {
        try {
            final StringWriter s =  new StringWriter();
            final BufferedWriter b = new BufferedWriter( s );
            final CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader( "Last Name", "First Name", "Attending?", "Primary", "Questions/Comments/Concerns" ).print( b );

            getRsvpRows().forEach( r -> {
                try {
                    r.printRsvpOutputRow( csvPrinter );
                } catch ( IOException e ) {
                    throw new RuntimeException( "Unable to output: " + r );
                }
            } );
            b.flush();

            response.type( "text/csv" );
            response.body( s.getBuffer().toString() );

            return response.body();
        } catch ( IOException e ) {
            throw new RuntimeException( "Encountered IO Exception.", e );
        }
    }

    private Stream<RsvpOutputRow> getRsvpRows() {
        return getFileLists().stream()
            .map( this::readObject )
            .flatMap( this::convertRsvp );
    }

    private List<String> getFileLists() {
        final ImmutableList.Builder<String> objectKeyList = ImmutableList.builder();
        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName( bucketName ).withMaxKeys( 15 );

        ListObjectsV2Result result;

        do {
            result = amazonS3.listObjectsV2( req );
            objectKeyList.addAll( result.getObjectSummaries().stream()
                .map( S3ObjectSummary::getKey )
                .collect( Collectors.toList() ) );
            req.setContinuationToken( result.getNextContinuationToken() );
        } while ( result.isTruncated() );

        return objectKeyList.build();
    }

    private RsvpResponse readObject( final String key ) {
        try {
            final String object = amazonS3.getObjectAsString( bucketName, key );

            return objectMapper.readValue( object, RsvpResponse.class );
        } catch ( IOException e ) {
            throw new RuntimeException( "Unable to read object!", e );
        }
    }

    private Stream<RsvpOutputRow> convertRsvp( final RsvpResponse rsvpResponse ) {
        final Stream<RsvpOutputRow> primary = Stream.of( new RsvpOutputRow(
            rsvpResponse.firstName,
            rsvpResponse.lastName,
            rsvpResponse.attendance,
            rsvpResponse.qcc,
            true ) );

        final Stream<RsvpOutputRow> guests = rsvpResponse.guests.stream().map( g -> new RsvpOutputRow(
            g.firstName,
            g.lastName,
            rsvpResponse.attendance,
            rsvpResponse.qcc,
            false ) );

        return Streams.concat( primary, guests );
    }

    public static void main( String[] args ) {
        final AWSCredentialsProvider awsCredentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        final AmazonS3 amazonS3 = AmazonS3Client.builder()
            .withRegion( "us-west-2" )
            .withCredentials( awsCredentialsProvider )
            .build();

        final Properties properties = readProperties();

        final RsvpReader rsvpReader = new RsvpReader( amazonS3, properties.getProperty( "bucketName" ), initializeOM() );

        Spark.get( "/rsvps.csv", rsvpReader::getRsvpFile );
    }

    private static Properties readProperties() {
        final Properties properties = new Properties();
        try (final InputStream stream = RsvpReader.class.getResourceAsStream( "/config.properties" ) ) {
            properties.load(stream);
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to initialize.", e );
        }
        return properties;
    }

    private static ObjectMapper initializeOM() {
        return new ObjectMapper();
    }
}
