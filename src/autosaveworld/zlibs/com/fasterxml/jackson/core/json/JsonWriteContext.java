package autosaveworld.zlibs.com.fasterxml.jackson.core.json;

import autosaveworld.zlibs.com.fasterxml.jackson.core.JsonGenerationException;
import autosaveworld.zlibs.com.fasterxml.jackson.core.JsonProcessingException;
import autosaveworld.zlibs.com.fasterxml.jackson.core.JsonStreamContext;

/**
 * Extension of {@link JsonStreamContext}, which implements core methods needed,
 * and also exposes more complete API to generator implementation classes.
 */
public class JsonWriteContext extends JsonStreamContext {
	// // // Return values for writeValue()

	public final static int STATUS_OK_AS_IS = 0;
	public final static int STATUS_OK_AFTER_COMMA = 1;
	public final static int STATUS_OK_AFTER_COLON = 2;
	public final static int STATUS_OK_AFTER_SPACE = 3; // in root context
	public final static int STATUS_EXPECT_VALUE = 4;
	public final static int STATUS_EXPECT_NAME = 5;

	/**
	 * Parent context for this context; null for root context.
	 */
	protected final JsonWriteContext _parent;

	// // // Optional duplicate detection

	protected final DupDetector _dups;

	/*
	 * /********************************************************** /* Simple
	 * instance reuse slots; speed up things /* a bit (10-15%) for docs with
	 * lots of small /* arrays/objects
	 * /**********************************************************
	 */

	protected JsonWriteContext _child = null;

	/**
	 * Name of the field of which value is to be parsed; only used for OBJECT
	 * contexts
	 */
	protected String _currentName;

	/**
	 * Marker used to indicate that we just received a name, and now expect a
	 * value
	 */
	protected boolean _gotName;

	/*
	 * /********************************************************** /* Life-cycle
	 * /**********************************************************
	 */

	protected JsonWriteContext(int type, JsonWriteContext parent,
			DupDetector dups) {
		super();
		_type = type;
		_parent = parent;
		_dups = dups;
		_index = -1;
	}

	protected JsonWriteContext reset(int type) {
		_type = type;
		_index = -1;
		_currentName = null;
		_gotName = false;
		if (_dups != null) {
			_dups.reset();
		}
		return this;
	}

	// // // Factory methods

	/**
	 * @deprecated Since 2.3; use method that takes argument
	 */
	@Deprecated
	public static JsonWriteContext createRootContext() {
		return createRootContext(null);
	}

	public static JsonWriteContext createRootContext(DupDetector dd) {
		return new JsonWriteContext(TYPE_ROOT, null, dd);
	}

	public JsonWriteContext createChildArrayContext() {
		JsonWriteContext ctxt = _child;
		if (ctxt == null) {
			_child = ctxt = new JsonWriteContext(TYPE_ARRAY, this,
					(_dups == null) ? null : _dups.child());
			return ctxt;
		}
		return ctxt.reset(TYPE_ARRAY);
	}

	public JsonWriteContext createChildObjectContext() {
		JsonWriteContext ctxt = _child;
		if (ctxt == null) {
			_child = ctxt = new JsonWriteContext(TYPE_OBJECT, this,
					(_dups == null) ? null : _dups.child());
			return ctxt;
		}
		return ctxt.reset(TYPE_OBJECT);
	}

	// // // Shared API

	@Override
	public final JsonWriteContext getParent() {
		return _parent;
	}

	@Override
	public final String getCurrentName() {
		return _currentName;
	}

	// // // API sub-classes are to implement

	/**
	 * Method that writer is to call before it writes a field name.
	 *
	 * @return Index of the field entry (0-based)
	 */
	public final int writeFieldName(String name) throws JsonProcessingException {
		_gotName = true;
		_currentName = name;
		if (_dups != null) {
			_checkDup(_dups, name);
		}
		return (_index < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
	}

	private final void _checkDup(DupDetector dd, String name)
			throws JsonProcessingException {
		if (dd.isDup(name)) {
			throw new JsonGenerationException("Duplicate field '" + name + "'");
		}
	}

	public final int writeValue() {
		// Most likely, object:
		if (_type == TYPE_OBJECT) {
			_gotName = false;
			++_index;
			return STATUS_OK_AFTER_COLON;
		}

		// Ok, array?
		if (_type == TYPE_ARRAY) {
			int ix = _index;
			++_index;
			return (ix < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
		}

		// Nope, root context
		// No commas within root context, but need space
		++_index;
		return (_index == 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_SPACE;
	}

	// // // Internally used abstract methods

	protected void appendDesc(StringBuilder sb) {
		if (_type == TYPE_OBJECT) {
			sb.append('{');
			if (_currentName != null) {
				sb.append('"');
				// !!! TODO: Name chars should be escaped?
				sb.append(_currentName);
				sb.append('"');
			} else {
				sb.append('?');
			}
			sb.append('}');
		} else if (_type == TYPE_ARRAY) {
			sb.append('[');
			sb.append(getCurrentIndex());
			sb.append(']');
		} else {
			// nah, ROOT:
			sb.append("/");
		}
	}

	// // // Overridden standard methods

	/**
	 * Overridden to provide developer writeable "JsonPath" representation of
	 * the context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		appendDesc(sb);
		return sb.toString();
	}
}
