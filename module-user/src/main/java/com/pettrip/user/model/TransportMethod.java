package com.pettrip.user.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "transport_methods")
@AttributeOverride(name = "id", column = @Column(name = "transport_method_id"))
public class TransportMethod extends BaseEntity {

  @Column(name = "transport_method_name", nullable = false, length = 50)
  private String transportMethodName;

  protected TransportMethod() {}

  public TransportMethod(String transportMethodName) {
    this.transportMethodName = transportMethodName;
  }

  public String getTransportMethodName() {
    return transportMethodName;
  }
}
