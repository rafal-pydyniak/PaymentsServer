package pl.pydyniak.payments.security

class ConfirmationToken {
	String token
	boolean activated = false
	User user

	static constraints = { token unique:true }

}
