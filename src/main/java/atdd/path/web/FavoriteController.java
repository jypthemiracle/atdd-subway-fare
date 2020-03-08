package atdd.path.web;

import atdd.path.application.FavoritePathService;
import atdd.path.application.FavoriteStationService;
import atdd.path.application.dto.FavoritePathRequestView;
import atdd.path.application.dto.FavoritePathResponseView;
import atdd.path.application.dto.FavoriteStationResponse;
import atdd.path.domain.FavoritePath;
import atdd.path.domain.Station;
import atdd.user.domain.User;
import atdd.user.web.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {
    @Autowired
    private FavoriteStationService favoriteStationService;

    @Autowired
    private FavoritePathService favoritePathService;

    @PostMapping("/stations")
    public ResponseEntity addFavoriteStation(@LoginUser User user, @RequestBody final long stationId) {
        FavoriteStationResponse response = favoriteStationService.addFavoriteStation(user.getEmail(), stationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stations")
    public ResponseEntity findFavoriteStations(@LoginUser User user) {
        List<FavoriteStationResponse> responses = favoriteStationService.findAll(user.getEmail());

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/stations/{id}")
    public ResponseEntity deleteFavoriteStation(@LoginUser User user, @PathVariable Long id) {
        favoriteStationService.deleteByIdAndOwner(user.getEmail(), id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/paths")
    public ResponseEntity addFavoritePath(@LoginUser final User user, @RequestBody FavoritePathRequestView view) {
        Station sourceStation = favoritePathService.findStationById(view.getSourceStationId());
        Station targetStation = favoritePathService.findStationById(view.getTargetStationId());

        FavoritePath favoritePath = favoritePathService.addFavoritePath(FavoritePath.builder()
                .owner(user.getId())
                .sourceStationId(view.getSourceStationId())
                .targetStationId(view.getTargetStationId()).build());

        return ResponseEntity.status(HttpStatus.CREATED).body(FavoritePathResponseView.of(favoritePath, sourceStation, targetStation));
    }

    @GetMapping("/paths")
    public ResponseEntity findFavoritePaths(@LoginUser final User user) {
        List<FavoritePath> favoritePaths = favoritePathService.findFavoritePath(user.getId());

        List<FavoritePathResponseView> result = new ArrayList<>();

        for (FavoritePath favoritePath : favoritePaths) {
            Station sourceStation = favoritePathService.findStationById(favoritePath.getSourceStationId());
            Station targetStation = favoritePathService.findStationById(favoritePath.getTargetStationId());

            result.add(FavoritePathResponseView.of(favoritePath, sourceStation, targetStation));
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/paths/{id}")
    public ResponseEntity deleteFavoritePath(@LoginUser final User user, @PathVariable Long id) {
        favoritePathService.deleteFavoritePath(user.getEmail(), id);

        return ResponseEntity.noContent().build();
    }
}