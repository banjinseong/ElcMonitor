//package charge.station.monitor.config.auth;
//
//
//import charge.station.monitor.domain.User;
//import charge.station.monitor.repository.UserRegionRepository;
//import charge.station.monitor.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    private final UserRegionRepository userRegionRepository;
//
//
//    @Override
//    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
//        User user = userRepository.findByUserId(Long.parseLong(userId))
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole()));
//
//        List<String> managedRegions = userRegionRepository.findByUser(user)
//                .stream()
//                .map(userRegion -> userRegion.getRegion().getRegionName())
//                .collect(Collectors.toList());
//
//        return new CustomUserDetails(user.getLoginId(), user.getPassword(), authorities, managedRegions);
//    }
//
//}
