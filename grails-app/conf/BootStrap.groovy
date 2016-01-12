import pl.pydyniak.payments.security.Role
import pl.pydyniak.payments.security.User
import pl.pydyniak.payments.security.UserRole

class BootStrap {
    def marshallersService

    def init = { servletContext ->
        initRoles()
        User user = User.findByUsername("rafal@pydyniak.pl")
        if (user==null)
            user = new User(username: "rafal@pydyniak.pl", password: "admin01", enabled: true).save(flush:true)

        UserRole.create(user, Role.findByAuthority("ROLE_ADMIN"), true)
        marshallersService.register()
    }
    def destroy = {
    }

    def initRoles() {
        Role.findOrCreateByAuthority("ROLE_ADMIN").save()
        Role.findOrCreateByAuthority("ROLE_USER").save()
    }

}
