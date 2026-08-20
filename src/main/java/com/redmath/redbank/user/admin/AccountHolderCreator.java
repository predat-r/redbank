package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.admin.dto.AccountHolderSummaryDto;

public interface AccountHolderCreator {

  AccountHolderSummaryDto createAccountHolder(User user, Long adminUserId);

  void deactivateAccountHolder(Long userId, Long adminUserId);

  void reactivateAccountHolder(Long userId, Long adminUserId);
}
