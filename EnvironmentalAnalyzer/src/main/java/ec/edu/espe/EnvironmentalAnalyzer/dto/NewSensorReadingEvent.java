package ec.edu.espe.EnvironmentalAnalyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewSensorReadingEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("sensorId")
    private String sensorId;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("value")
    private Double value;
    
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
