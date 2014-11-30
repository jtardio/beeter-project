package edu.upc.eetac.dsa.smachado.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import edu.upc.eetac.dsa.smachado.beeter.api.model.Sting;
import edu.upc.eetac.dsa.smachado.beeter.api.model.StingCollection;
import edu.upc.eetac.dsa.smachado.beeter.api.model.User;

@Path("/users")
public class UserResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
 
	private final static String GET_USER_BY_USERNAME_QUERY = "select * from users where username=?";
	private final static String INSERT_USER_INTO_USERS = "insert into users values(?, MD5(?), ?, ?)";
	private final static String INSERT_USER_INTO_USER_ROLES = "insert into user_roles values (?, 'registered')";
	private final static String GET_USER_DATA = "select name, email from users where username=?";
	
 
	@POST
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User createUser(User user) {
		validateUser(user);
 
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmtGetUsername = null;
		PreparedStatement stmtInsertUserIntoUsers = null;
		PreparedStatement stmtInsertUserIntoUserRoles = null;
		try {
			stmtGetUsername = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmtGetUsername.setString(1, user.getUsername());
 
			ResultSet rs = stmtGetUsername.executeQuery();
			if (rs.next())
				throw new WebApplicationException(user.getUsername()
						+ " already exists.", Status.CONFLICT);
			rs.close();
 
			conn.setAutoCommit(false);
			stmtInsertUserIntoUsers = conn
					.prepareStatement(INSERT_USER_INTO_USERS);
			stmtInsertUserIntoUserRoles = conn
					.prepareStatement(INSERT_USER_INTO_USER_ROLES);
 
			stmtInsertUserIntoUsers.setString(1, user.getUsername());
			stmtInsertUserIntoUsers.setString(2, user.getPassword());
			stmtInsertUserIntoUsers.setString(3, user.getName());
			stmtInsertUserIntoUsers.setString(4, user.getEmail());
			stmtInsertUserIntoUsers.executeUpdate();
 
			stmtInsertUserIntoUserRoles.setString(1, user.getUsername());
			stmtInsertUserIntoUserRoles.executeUpdate();
 
			conn.commit();
		} catch (SQLException e) {
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException e1) {
				}
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmtGetUsername != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUsers != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUserRoles != null)
					stmtGetUsername.close();
				conn.setAutoCommit(true);
				conn.close();
			} catch (SQLException e) {
			}
		}
		user.setPassword(null);
		return user;
	}

//------------Cacheo de usuario------------------------------------------------Ejercicio 1.2, primer requisito
	@GET
	@Path("/{username}")
	@Produces(MediaType.BEETER_API_USER)
	public Response getUser(@PathParam("username") String username,
			@Context Request request) {
		// Create CacheControl
		CacheControl cc = new CacheControl();
		User usuario = datosdeusuario(username);
		
		//Variable para detectar cambios en los datos de usuario y así identificar si se puede cachear
		String control = DigestUtils.md5Hex(usuario.getName()+usuario.getEmail());
	 
		// Calculate the ETag on last modified date of user resource
		EntityTag eTag = new EntityTag(control);
	 
		// Verify if it matched with etag available in http request
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);
	 
		// If ETag matches the rb will be non-null;
		// Use the rb to return the response without any further processing
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}
	 
		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(usuario).cacheControl(cc).tag(eTag);
	 
		return rb.build();
	}
 
	private void validateUser(User user) {
		if (user.getUsername() == null)
			throw new BadRequestException("username cannot be null.");
		if (user.getPassword() == null)
			throw new BadRequestException("password cannot be null.");
		if (user.getName() == null)
			throw new BadRequestException("name cannot be null.");
		if (user.getEmail() == null)
			throw new BadRequestException("email cannot be null.");
	}
 
	@Path("/login")
	@POST
	@Produces(MediaType.BEETER_API_USER)
	@Consumes(MediaType.BEETER_API_USER)
	public User login(User user) {
		if (user.getUsername() == null || user.getPassword() == null)
			throw new BadRequestException(
					"username and password cannot be null.");
 
		String pwdDigest = DigestUtils.md5Hex(user.getPassword());
		String storedPwd = getUserFromDatabase(user.getUsername(), true)
				.getPassword();
 
		user.setLoginSuccessful(pwdDigest.equals(storedPwd));
		user.setPassword(null);
		return user;
	}


 
	private User getUserFromDatabase(String username, boolean password) {
		User user = new User();
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
 
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, username);
 
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user.setUsername(rs.getString("username"));
				if (password)
					user.setPassword(rs.getString("userpass"));
				user.setEmail(rs.getString("email"));
				user.setName(rs.getString("name"));
			} else
				throw new NotFoundException(username + " not found.");
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
 
		return user;
	}
	
	private User datosdeusuario(String username) {
		User usuario = new User();
	 
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
	 
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, String.valueOf(username));
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				usuario.setUsername(rs.getString("username"));
				usuario.setName(rs.getString("name"));
				usuario.setEmail(rs.getString("email"));
				
			} else {
				throw new NotFoundException("There's no user with Username "
						+ username);
						}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException("Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}
		}
	 
		return usuario;
	}
	
	
}