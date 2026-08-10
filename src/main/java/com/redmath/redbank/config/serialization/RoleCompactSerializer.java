package com.redmath.redbank.config.serialization;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.redmath.redbank.user.role.Role;
import com.redmath.redbank.user.role.RoleName;

public final class RoleCompactSerializer implements CompactSerializer<Role> {

  @Override
  public Role read(CompactReader reader) {
    return Role.builder()
        .id(reader.readNullableInt64("id"))
        .name(RoleName.valueOf(reader.readString("name")))
        .build();
  }

  @Override
  public void write(CompactWriter writer, Role role) {
    writer.writeNullableInt64("id", role.getId());
    writer.writeString("name", role.getName().name());
  }

  @Override
  public String getTypeName() {
    return "redbank.role";
  }

  @Override
  public Class<Role> getCompactClass() {
    return Role.class;
  }
}
