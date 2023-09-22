@Controller
public class CookieController {

    @RequestMapping(value = "/cookie1", method = "GET")
    public void setCookie(@RequestParam String value, HttpServletResponse response) {
        Cookie cookie = new Cookie("cookie", value);
        response.addCookie(cookie);
    }

    @RequestMapping(value = "/cookie2", method = "GET")
    public void setSecureCookie(@RequestParam String value, HttpServletResponse response) {
        Cookie cookie = new Cookie("cookie", value);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    @RequestMapping(value = "/cookie3", method = "GET")
    public void setSecureHttponlyCookie(@RequestParam String value, HttpServletResponse response) {
        Cookie cookie = new Cookie("cookie", value);
        cookie.setSecure(true);
        // ok: cookie-httponly-false
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @RequestMapping(value = "/cookie4", method = "GET")
    public void explicitDisable(@RequestParam String value, HttpServletResponse response) {
        Cookie cookie = new Cookie("cookie", value);
        cookie.setSecure(false);
        // ruleid: cookie-httponly-false
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }
}
