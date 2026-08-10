package com.redmath.redbank.config.serialization;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountStatus;

public final class AccountHolderCompactSerializer implements CompactSerializer<AccountHolder> {

  @Override
  public AccountHolder read(CompactReader reader) {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(reader.readNullableInt64("id"));
    accountHolder.setAccountNumber(reader.readString("accountNumber"));
    accountHolder.setCurrency(reader.readString("currency"));
    accountHolder.setAccountStatus(AccountStatus.valueOf(reader.readString("accountStatus")));
    return accountHolder;
  }

  @Override
  public void write(CompactWriter writer, AccountHolder accountHolder) {
    writer.writeNullableInt64("id", accountHolder.getId());
    writer.writeString("accountNumber", accountHolder.getAccountNumber());
    writer.writeString("currency", accountHolder.getCurrency());
    writer.writeString("accountStatus", accountHolder.getAccountStatus().name());
  }

  @Override
  public String getTypeName() {
    return "redbank.account-holder";
  }

  @Override
  public Class<AccountHolder> getCompactClass() {
    return AccountHolder.class;
  }
}
