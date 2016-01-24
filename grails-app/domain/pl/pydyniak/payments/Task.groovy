package pl.pydyniak.payments

import pl.pydyniak.payments.security.User

class Task {
	String name
	String description
	Date realisationDate
	Integer priority = 3 //1 -lowest, 5 - highest
	Double amount
	Long timestamp
	Long lastUpdated

	Boolean enabled=true
	Boolean deleted=false
	
	User user

    static constraints = {
		description nullable:true
		realisationDate nullable:true
		amount nullable:true
    }
}
