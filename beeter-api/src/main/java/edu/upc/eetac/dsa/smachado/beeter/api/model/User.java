package edu.upc.eetac.dsa.smachado.beeter.api.model;

import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.smachado.beeter.api.MediaType;
import edu.upc.eetac.dsa.smachado.beeter.api.StingResource;
import edu.upc.eetac.dsa.smachado.beeter.api.UserResource;

public class User {
	/*@InjectLinks({
		@InjectLink(resource = UserResource.class, style = Style.ABSOLUTE, rel = "users", title = "Latest users", type = MediaType.BEETER_API_USER_COLLECTION),
		@InjectLink(resource = UserResource.class, style = Style.ABSOLUTE, rel = "self edit", title = "User", type = MediaType.BEETER_API_USER, method = "getUser", bindings = @Binding(name = "name", value = "${instance.name}")) })
	private List<Link> links;*/
	private String username;
	private String password;
	private String name;
	private String email;
	private boolean loginSuccessful;

 
	public String getUsername() {
		return username;
	}
 
	public void setUsername(String username) {
		this.username = username;
	}
 
	public String getPassword() {
		return password;
	}
 
	public void setPassword(String password) {
		this.password = password;
	}
 
	public String getName() {
		return name;
	}
 
	public void setName(String name) {
		this.name = name;
	}
 
	public String getEmail() {
		return email;
	}
 
	public void setEmail(String email) {
		this.email = email;
	}
 
	public boolean isLoginSuccessful() {
		return loginSuccessful;
	}
 
	public void setLoginSuccessful(boolean loginSuccessful) {
		this.loginSuccessful = loginSuccessful;
	}
	/*public List<Link> getLinks() {
		return links;
	}
 
	public void setLinks(List<Link> links) {
		this.links = links;
	}*/


}