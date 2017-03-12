var fs = require('fs');

var dependencies = [];
var alreadyGenerated = [];
var anonymousTypesToGenerate = [];
var anonymousTypeCount = 0;

var primitiveObjectTypes = {
    'string': 'String',
    'boolean': 'boolean',
    'integer': 'int',
    'number': 'double',
    'any': 'Object'
};

var primitiveOptionalObjectTypes = {
    'string': 'String',
    'boolean': 'Boolean',
    'integer': 'Integer',
    'number': 'Double',
    'any': 'Object'
};

function tab(amount) {
    amount = amount || 1;

    // two spaces per indent
    return new Array(amount + 1).join('  ');
}

function ret(amount) {
    amount = amount || 1;
    return new Array(amount + 1).join('\r\n');
}

function getAnonymousTypeName() {
    return 'AnonType' + anonymousTypeCount++;
}

function isPrimitive(typeName) {
    return primitiveObjectTypes.hasOwnProperty(typeName);
}

function isPrimitiveOrArray(typeName) {
    return isPrimitive(typeName) || typeName == 'array';
}

function resolveName(name) {
    var containsDot = name.indexOf('.') >= 0;
    var split = name.split('.');

    return {
        domain: containsDot ? split[0] : '',
        name: containsDot ? split[1] : name
    };
}

function findCommandOrEventDefinition(resolved) {
    var match = null;

    documentation.domains.forEach(function (domain) {
        if (!resolved.domain || resolved.domain == domain.domain) {
            if (domain.types) {
                var filterFunc = function(commandOrEvent) {
                    return commandOrEvent.name == resolved.name;
                };

                var matches = domain.commands.filter(filterFunc);
                matches = matches.concat(domain.events.filter(filterFunc));

                if (matches.length > 0) {
                    match = matches[0];
                    resolved.domain = domain.domain;
                }
            }
        }
    });

    return match;
}

function findTypeDefinition(resolved) {
    var match = null;

    documentation.domains.forEach(function (domain) {
        if (!resolved.domain || resolved.domain == domain.domain) {
            if (domain.types) {
                var matches = domain.types.filter(function (type) {
                    return type.id == resolved.name;
                });

                if (matches.length > 0) {
                    match = matches[0];
                    resolved.domain = domain.domain;
                }
            }
        }
    });

    return match;
}

function generateJavaTypeEquivalent(currentType, prop) {
    if (prop.hasOwnProperty('type')) {
        // if it's a primitive type, then just map to the java equivalent
        var type = primitiveObjectTypes.hasOwnProperty(prop.type) ? primitiveObjectTypes[prop.type] : prop.type;

        // if the property is optional, it is nullable
        if (prop.optional && primitiveOptionalObjectTypes.hasOwnProperty(prop.type)) {
            type = primitiveOptionalObjectTypes[prop.type];
        }

        if (prop.type == 'array') {
            if (prop.items.hasOwnProperty('$ref')) {
                if (currentType != prop.items.$ref) {
                    addDependencyIfNotGenerated(prop.items.$ref);
                }
                type = 'List<' + prop.items.$ref + '>';
            } else {
                type = getAnonymousTypeName();
                var typeDef = {
                    id: type,
                    type: prop.items.type,
                    properties: prop.items.properties
                };

                type = 'List<' + type + '>';

                anonymousTypesToGenerate.push(typeDef);

            }
        }

        return type;
    } else {
        var typeDefinition = findTypeDefinition(resolveName(prop.$ref));

        if (!isPrimitiveOrArray(typeDefinition.type)) {
            if (typeDefinition.type != currentType) {
                addDependencyIfNotGenerated(prop.$ref);
            }
            return prop.$ref;
        } else {
            var type = typeDefinition.type;
            var resolvedType = primitiveObjectTypes[type] || type;

            if (prop.optional) {
                if (primitiveOptionalObjectTypes.hasOwnProperty(type)) {
                    resolvedType = primitiveOptionalObjectTypes[type];
                }
            }

            if (type == 'array') {
                addDependencyIfNotGenerated(typeDefinition.items.$ref);
                return 'List<' + typeDefinition.items.$ref + '>';
            }

            return resolvedType;
        }

    }
}

function generateJavaClassForType(typeDefinition) {
    var result = 'public static class ' + typeDefinition.id + ' {';

    if (typeDefinition.properties != undefined) {
        typeDefinition.properties.forEach(function (prop) {
            result += ret() + tab() + '\@JsonProperty';
    
            if (!prop.optional) {
                result += '(required = true)';
            }
    
            result += ret();
    
            result += tab() + 'public ' + generateJavaTypeEquivalent(typeDefinition.id, prop) + ' ' + prop.name + ';' + ret();
        });
    } else {
        result += ret();
    }

    result += '}';

    return result;
}

function addDependencyIfNotGenerated(dependencyString) {
    var resolved = resolveName(dependencyString);

    if (!dependencyExists(resolved) && !isPrimitiveOrArray(findTypeDefinition(resolved).type)) {
        dependencies.push(resolved);
    }
}

function dependencyExists(resolvedDependency) {
    return dependencies.filter(function (existing) {
            return resolvedDependency.name == existing.name && resolvedDependency.domain == existing.domain;
        }).length != 0 ||
        alreadyGenerated.filter(function (existing) {
            return resolvedDependency.name == existing.name && resolvedDependency.domain == existing.domain;
        }).length != 0;
}

function generateCommandOrEvent(commandDef) {
    var className = commandDef.name.charAt(0).toUpperCase() + commandDef.name.slice(1);

    var hasParams = !!commandDef.parameters;
    var hasReturns = !!commandDef.returns;

    var paramsTypeName = className + 'Request';
    var returnsTypeName = className + 'Response';

    var result = '' +
        '@ChromeDevtoolsMethod' + ret() +
        'public JsonRpcResult ' + commandDef.name + '(JsonRpcPeer peer, JSONObject params) {' + ret();

    if (hasParams) {
        result += '' +
            tab(1) + 'final ' + paramsTypeName + ' = mObjectMapper.convertValue' + ret() +
            tab(2) + 'params,' + ret() +
            tab(2) + paramsTypeName + '.type);' + ret(2);
    }

    if (hasReturns) {
        result += '' +
            tab() + 'final ' + returnsTypeName + 'response = new ' + returnsTypeName + '();' + ret() +
            tab() + 'return response;' + ret();
    }

    result += '}' + ret(2);

    if (hasParams) {
        result += generateType({
                id: paramsTypeName,
                properties: commandDef.parameters
            }) + ret(2);
    }

    if (hasReturns) {
        result += generateType({
            id: returnsTypeName,
            properties: commandDef.returns
        });
    }

    return result;
}

function generateDependencies() {
    var result = '';

    while (dependencies.length > 0 || anonymousTypesToGenerate.length > 0) {
        while (dependencies.length > 0) {
            result += ret(2)
                + generateType(
                    findTypeDefinition(
                        dependencies.pop()));
        }

        while (anonymousTypesToGenerate.length > 0) {
            result += ret(2) +
                generateType(anonymousTypesToGenerate.pop());
        }
    }

    return result;
}

function generateType(typeDef) {
    alreadyGenerated.push(resolveName(typeDef.id));

    if (isPrimitiveOrArray(typeDef.type)) {
        return 'The type \'' + typeDef.id + '\' is a primitive type (' + typeDef.type + '), so no class generation is necessary.';
    }

    var result = generateJavaClassForType(typeDef);

    result += generateDependencies();

    return result;
}

function generate(name) {
    var resolved = resolveName(name);
    var command = findCommandOrEventDefinition(resolved);

    if (command != null) {
        return generateCommandOrEvent(command);
    }

    var type = findTypeDefinition(resolved);

    if (type != null) {
        return generateType(type);
    }

    return 'no command or type \'' + name + '\' found';
}

// first two args are path to node and to this file
var arguments = process.argv.slice(2);


if (arguments.length == 0) {
    console.log('usage:' + ret() +
        tab() + 'node scraper.js path_to_protocol_json name_of_method_or_type' + ret() +
        '  node scraper.js path_to_protocol_json domain.name_of_method_or_type' + ret() +
        'description:' + ret() +
        tab() + 'This script generates Java code representing a type or method defined in `protocol.json`,' +
        ' which can be found at: https://code.google.com/p/chromium/codesearch#chromium/src/third_party/WebKit/Source/devtools/protocol.json')
} else {
    var documentation = JSON.parse(fs.readFileSync(arguments[0], {encoding: 'utf8'}));
    console.log(generate(arguments[1]));
}

