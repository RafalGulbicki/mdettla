<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<h2>Submit a new link</h2>

<form:form modelAttribute="submission" method="POST">
	<table>
		<tr>
			<td>Title:</td>
			<td><form:input path="title" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td></td><td><input type="submit" value="Submit" /></td>
		</tr>
	</table>
</form:form>
