#scraper
Example:

```
➜ node scraper.js protocol.json Debugger.FunctionDetails
```

... generates the `FunctionDetails` object defined in the `Debugger` namespace using the file `protocol.json`, which is downloaded from google code. 
Note: if not specified, the domain is implicit.

Here's the code that was generated:

```
public static class FunctionDetails {
	@JsonProperty
	public Location location;

	@JsonProperty(required = true)
	public String functionName;

	@JsonProperty(required = true)
	public boolean isGenerator;

	@JsonProperty
	public List<Scope> scopeChain;
}

public static class Scope {
	@JsonProperty(required = true)
	public String type;

	@JsonProperty(required = true)
	public Runtime.RemoteObject object;
}

... and so on (omited for brevity)
```
