package nextstep.subway.auth.ui.interceptor.authorization;

import java.util.Arrays;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import nextstep.subway.auth.application.UserDetailsService;
import nextstep.subway.auth.domain.Authentication;
import nextstep.subway.auth.domain.AuthenticationPrincipal;
import nextstep.subway.auth.infrastructure.SecurityContextHolder;
import nextstep.subway.members.member.domain.LoginMember;

public class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserDetailsService userDetailsService;

    public AuthenticationPrincipalArgumentResolver(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return userDetailsService.anonymousMember();
        }
        if (authentication.getPrincipal() instanceof Map) {
            return extractPrincipal(parameter, authentication);
        }

        return authentication.getPrincipal();
    }

    public static Object toObject(Class clazz, String value) {
        if (Boolean.class == clazz)
            return Boolean.parseBoolean(value);
        if (Byte.class == clazz)
            return Byte.parseByte(value);
        if (Short.class == clazz)
            return Short.parseShort(value);
        if (Integer.class == clazz)
            return Integer.parseInt(value);
        if (Long.class == clazz)
            return Long.parseLong(value);
        if (Float.class == clazz)
            return Float.parseFloat(value);
        if (Double.class == clazz)
            return Double.parseDouble(value);
        if (String.class == clazz)
            return value;
        return value;
    }

    private Object extractPrincipal(MethodParameter parameter, Authentication authentication) {
        try {
            Map<String, String> principal = (Map)authentication.getPrincipal();

            Object[] params = Arrays.stream(LoginMember.class.getDeclaredFields())
                .map(it -> toObject(it.getType(), principal.get(it.getName())))
                .toArray();

            return LoginMember.class.getConstructors()[0].newInstance(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
