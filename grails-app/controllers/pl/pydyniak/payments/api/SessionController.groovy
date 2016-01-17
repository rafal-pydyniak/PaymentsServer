package pl.pydyniak.payments.api

import grails.converters.JSON
import grails.rest.RestfulController
import grails.transaction.Transactional
import org.apache.commons.lang.RandomStringUtils
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import pl.pydyniak.payments.security.ConfirmationToken
import pl.pydyniak.payments.security.Role
import pl.pydyniak.payments.security.User
import pl.pydyniak.payments.security.UserRole

@RestApi(name = "Session resource", description = "Methods for managing user session")
class SessionController extends RestfulController {
    def springSecurityService
    def mailService


    static allowedMethods = [register: 'POST', edit: 'PUT', delete: 'DELETE', blockUser: 'PUT', changePartnership: 'PUT',
    authEmail: 'GET']

    String tokenCharset = (('A'..'Z') + ('0'..'9')).join()
    Integer tokenLength = 32

    static responseFormats = ['json']



    def index() {
        respond info: "Text"
    }

    @RestApiMethod(description = "Register new user, returns 201 if user has been created", verb = RestApiVerb.POST)
    @RestApiErrors(apierrors = [
            @RestApiError(code = "405", description = "Bad method, only POST method is allowed"),
            @RestApiError(code = "400", description = "Bad JSON - not enough data or it's application/json is not set"),
            @RestApiError(code = "409", description = "Conflict. User with this username already exists in database")
    ])
    @Transactional
    def register(User user) {
        println "Register method"

        def json = request.JSON

        if (user.username == null) {
            response.status = 400
            render([Error: "Username can't be null!"] as JSON)
            return
        }
        if (user.password == null) {
            response.status = 400
            render([Error: "Password can't be null!"] as JSON)
            return
        }

        if (User.findByUsername(user.username) != null) {
            render(status: 409)
            return
        }

        if (user.password.size() < 6) {
            respond status: "Password has to have at least 6 characters"
            return
        }



        String randomToken = RandomStringUtils.random(tokenLength, tokenCharset.toCharArray())
        if (ConfirmationToken.findByToken(randomToken) != null) {
            while (ConfirmationToken.findByToken(randomToken) != null) {
                randomToken = RandomStringUtils.random(tokenLength, tokenCharset.toCharArray())
            }
        }
        user.save(flush: true, failOnError: true)
        def role = Role.findByAuthority("ROLE_USER")


        UserRole.create user, role, true

        new ConfirmationToken(
                token: randomToken,
                user: user
        ).save(flush: true, failOnError: true)

        sendConfirmationEmail(user, randomToken)
        render(status: 201)
        return

    }

    @RestApiMethod(description = "Edit user account informations", verb = RestApiVerb.PUT)
    @RestApiErrors(apierrors = [
            @RestApiError(code = "405", description = "Bad method, only POST method is allowed"),
            @RestApiError(code = "401", description = "Unauthorized. User is not logged in"),
            @RestApiError(code = "400", description = "Bad request. Request json is empty")
    ])
    @Transactional
    def edit() {
        if (!springSecurityService.isLoggedIn()) {
            render(status: 401)
            return
        }


        User currentUser = springSecurityService.currentUser

        def json = request.JSON

        if (json.isEmpty()) {
            render(status: 400)
            return
        }

        if (json.password != null) {
            currentUser.password = json.password
        }

        currentUser.save(flush: true, failOnError: true)

        respond status: "OK"
        return
    }

    def sendConfirmationEmail(User user, String token) {
        def serverUrl = grailsApplication.config.serverURL
        def authenticateResourceUrl = "api/session/auth/"
        def emailBody = "Go to the site to authenticate your account " + serverUrl + authenticateResourceUrl
        def emailSubject = serverUrl + " account verification"

        System.out.println("Sending confirmation email to " + user.username)
        emailBody + token
        mailService.sendMail {
            to(user.username)
            subject emailSubject
            body(emailBody + token)
        }

        println "Confirmation email sent"
    }

    @RestApiMethod(description = "Verificate email", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "Token", type = "String", paramType = RestApiParamType.PATH,
                    description = "Verification token that is sent by email")
    ])
    @Transactional
    def authEmail(String token) {
        def confirmationToken = ConfirmationToken.findByToken(token)

        if (confirmationToken == null) {
            respond status: "Invalid token!"
            return
        }

        if (confirmationToken.activated == true) {
            respond status: "Token has been already used!"
            return
        }

        confirmationToken.user.enabled = true
        confirmationToken.user.save(flush: true, failOnError: true)

        confirmationToken.activated = true
        confirmationToken.save(flush: true, failOnError: true)

        respond status: "User has been activated"
        return
    }


    @RestApiMethod(description = "Delete account (can be done by any user or by administrator to delete someone's account", verb = RestApiVerb.DELETE)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH,
                    description = "Id of user to delete. If it's 0 then currently logged user will be deleted.")
    ])
    @RestApiErrors(apierrors = [
            @RestApiError(code = "405", description = "Bad method, only DELETE method is allowed"),
            @RestApiError(code = "204", description = "User was not found")
    ])
    @Transactional
    def delete(int id) {
        if (springSecurityService.isLoggedIn()) {
            User currentUser = springSecurityService.getCurrentUser()

            def userRole = UserRole.findByUser(currentUser)
            if (id > 0) {
                if (userRole.role.authority == "ROLE_ADMIN") {

                    def userToDelete = User.findById(id)
                    if (userToDelete == null) {
                        render(status: 204)
                        return
                    }

                    userToDelete.accountLocked = true
                    userToDelete.save(flush: true, failOnError: true)

                    respond status: "User has been deleted"
                    return
                } else {
                    respond status: "Only administrator can delete other user!"
                    return
                }
            }

            if (id == 0) {
                currentUser.deleted = true
                respond status: "User deleted"
                return
            }

            //			if (currentUser.au)
        }
        respond status: "You need to be logged in to access this page"
        return
    }
}
