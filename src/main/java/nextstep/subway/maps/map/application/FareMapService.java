package nextstep.subway.maps.map.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nextstep.subway.maps.line.application.LineService;
import nextstep.subway.maps.line.domain.Line;
import nextstep.subway.maps.line.domain.LineStation;
import nextstep.subway.maps.line.domain.Money;
import nextstep.subway.maps.line.dto.LineResponse;
import nextstep.subway.maps.line.dto.LineStationResponse;
import nextstep.subway.maps.map.domain.DiscountPolicyType;
import nextstep.subway.maps.map.domain.PathType;
import nextstep.subway.maps.map.domain.SubwayPath;
import nextstep.subway.maps.map.dto.FarePathResponse;
import nextstep.subway.maps.map.dto.MapResponse;
import nextstep.subway.maps.map.dto.PathResponseAssembler;
import nextstep.subway.maps.station.application.StationService;
import nextstep.subway.maps.station.domain.Station;
import nextstep.subway.maps.station.dto.StationResponse;
import nextstep.subway.members.member.domain.LoginMember;

@Service
@Transactional(readOnly = true)
public class FareMapService {

    private final LineService lineService;
    private final StationService stationService;
    private final PathService pathService;
    private final DiscountFareCalculator fareCalculator;

    public FareMapService(LineService lineService, StationService stationService, PathService pathService,
        DiscountFareCalculator fareCalculator) {
        this.lineService = lineService;
        this.stationService = stationService;
        this.pathService = pathService;
        this.fareCalculator = fareCalculator;
    }

    public MapResponse findSubwayMap() {
        List<Line> lines = lineService.findLines();
        Map<Long, Station> stationsWithIdsOfAllLines = extractStationsWithIdsOfAllLines(lines);
        List<LineResponse> lineResponses = lines.stream()
            .map(line -> LineResponse.of(line, extractLineStationResponsesOfLine(line, stationsWithIdsOfAllLines)))
            .collect(Collectors.toList());

        return new MapResponse(lineResponses);
    }

    public FarePathResponse findPathWithFare(Long source, Long target, PathType pathType) {
        List<Line> lines = lineService.findLines();
        SubwayPath shortestPath = pathService.findPath(lines, source, target, PathType.DISTANCE);
        Money fare = fareCalculator.calculate(shortestPath);
        if (pathType == PathType.DISTANCE) {
            return assemblePathResponse(shortestPath, fare);
        }
        SubwayPath subwayPath = pathService.findPath(lines, source, target, pathType);
        return assemblePathResponse(subwayPath, fare);
    }

    public FarePathResponse findPathWithFare(LoginMember loginMember, Long source, Long target, PathType pathType) {
        List<Line> lines = lineService.findLines();
        SubwayPath shortestPath = pathService.findPath(lines, source, target, PathType.DISTANCE);
        Money fare = fareCalculator.calculate(shortestPath, DiscountPolicyType.ofAge(loginMember.getAge()));
        if (pathType == PathType.DISTANCE) {
            return assemblePathResponse(shortestPath, fare);
        }
        SubwayPath subwayPath = pathService.findPath(lines, source, target, pathType);
        return assemblePathResponse(subwayPath, fare);
    }

    private Map<Long, Station> extractStationsWithIdsOfAllLines(List<Line> lines) {
        List<Long> stationIds = lines.stream()
            .flatMap(line -> line.getStationInOrder().stream())
            .map(LineStation::getStationId)
            .collect(Collectors.toList());

        return stationService.findStationsByIds(stationIds);
    }

    private List<LineStationResponse> extractLineStationResponsesOfLine(Line line, Map<Long, Station> stations) {
        return line.getStationInOrder().stream()
            .map(station -> LineStationResponse.of(
                line.getId(), station, StationResponse.of(stations.get(station.getStationId()))
            )).collect(Collectors.toList());
    }

    private FarePathResponse assemblePathResponse(SubwayPath subwayPath, Money fare) {
        Map<Long, Station> stations = stationService.findStationsByIds(subwayPath.extractStationId());
        return PathResponseAssembler.assemble(subwayPath, stations, fare);
    }
}
