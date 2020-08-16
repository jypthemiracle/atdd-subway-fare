package nextstep.subway.maps.line.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Embeddable
public class LineStations {
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "line_id", foreignKey = @ForeignKey(name = "fk_line_station_to_line"))
    private List<LineStation> lineStations = new ArrayList<>();

    public List<LineStation> getLineStations() {
        return lineStations;
    }

    public List<LineStation> getStationsInOrder() {
        // 출발지점 찾기
        Optional<LineStation> preLineStation = lineStations.stream()
            .filter(it -> it.getPreStationId() == null)
            .findFirst();

        List<LineStation> result = new ArrayList<>();
        while (preLineStation.isPresent()) {
            LineStation preStationId = preLineStation.get();
            result.add(preStationId);
            preLineStation = lineStations.stream()
                .filter(it -> it.getPreStationId() == preStationId.getStationId())
                .findFirst();
        }
        return result;
    }

    public void add(LineStation lineStation) {
        checkValidation(lineStation);

        lineStations.stream()
            .filter(it -> it.getPreStationId() == lineStation.getPreStationId())
            .findFirst()
            .ifPresent(it -> it.updatePreStationTo(lineStation.getStationId()));

        lineStations.add(lineStation);
    }

    private void checkValidation(LineStation lineStation) {
        if (lineStation.getStationId() == null) {
            throw new RuntimeException();
        }

        if (lineStations.stream().anyMatch(it -> it.isSame(lineStation))) {
            throw new RuntimeException();
        }
    }

    public void removeByStationId(Long stationId) {
        LineStation lineStation = lineStations.stream()
            .filter(it -> it.getStationId() == stationId)
            .findFirst()
            .orElseThrow(RuntimeException::new);

        lineStations.stream()
            .filter(it -> it.getPreStationId() == stationId)
            .findFirst()
            .ifPresent(it -> it.updatePreStationTo(lineStation.getPreStationId()));

        lineStations.remove(lineStation);
    }

    public Integer calculateDurationFromStart(Long stationId) {
        List<LineStation> stationsInOrder = getStationsInOrder();
        int totalDuration = 0;
        for (LineStation lineStation : stationsInOrder) {
            if (Objects.equals(lineStation.getStationId(), stationId)) {
                break;
            }
            totalDuration += lineStation.getDuration();
        }
        return totalDuration;
    }
}
