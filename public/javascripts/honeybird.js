function Event(data) {
  this.service    = ko.observable(data.service);
  this.source     = ko.observable(data.source);
  this.etype      = ko.observable(data.etype);
  this.user       = ko.observable(data.user);
  this.icon       = ko.observable("icon-info-sign");
  this.date_begun = ko.observable(moment(data.date_begun, "YYYYMMDDTHHmmssZ"));
  this.date_ended = ko.observable(moment(data.date_ended, "YYYYMMDDTHHmmssZ"));
  this.ago        = ko.observable(moment(data.date_begun, "YYYYMMDDTHHmmssZ").fromNow());
  // Predicate for controlling the presence of the source bits.
  this.hasSource  = ko.computed(function() {
    if(this.source() != "") {
      return true;
    } else {
      return false;
    }
  }, this);
  // Compute a bootstrap icon.
  if(this.etype() == "incident") {
    this.icon("icon-fire");
  } else if(this.etype() == "deployment") {
    this.icon("icon-exclamation-sign");
  } else if (this.etype() == "notice") {
    this.icon("icon-bullhorn")
  }
}

function Facet(data) {
  this.value    = ko.observable(data.term);
  this.count    = ko.observable(data.count);
}

function FacetCollection(name, data) {
  this.name   = ko.observable(name);
  this.facets = ko.observableArray(_.map(data["terms"], function(f) { return new Facet(f) }))
}

function Filter(name, value) {
  this.name = ko.observable(name);
  this.value = ko.observable(value);
  // Make a pretty name for displaying
  this.linkName = ko.computed(function() {
    return this.name() + ": " + this.value();
  }, this);
}

function ResultsViewModel() {
  var self = this;
  self.url            = ko.observable(URI("/search"))
  self.events         = ko.observableArray([])
  self.facetCollection= ko.observableArray([])

  // Computer filters based on our current search url.
  self.filters        = ko.computed(function() {
    var effs = self.url().search(true);
    var filts = _.map(_.keys(effs), function(eff) { return new Filter(eff, effs[eff]) });
    return filts;
  }, this);

  // Show all the events again.
  function showEvents() {

    // Use the current search URL
    $.getJSON(self.url().toString())
      .done(function(data) {

        var events = $.map(data['hits']['hits'], function(ev) { return new Event(ev['_source']) });
        self.events(events);

        var terms =_.keys(data["facets"]);
        var facets = _.map(
          // Filter out facets with only one value in them, no reason to show that.
          _.filter(terms, function(f) { return data["facets"][f]["terms"].length > 1 }),
          function(t) { return new FacetCollection(t, data["facets"][t]) }
        );
        self.facetCollection(facets);
      })
      .fail(function(foo) { alert("Error fetching events.") });
  }

  // Add a filter from the search URL
  self.addFilter = function(facet, parent) {
    self.url(self.url().addSearch(parent.name(), facet.value()));
  }

  // Remove a filter from the search URL
  self.removeFilter = function(filter) {
    self.url(self.url().removeSearch(filter.name(), filter.value()));
  }

  // Refresh anytime the search URL is changed.
  self.url.subscribe(function(newvalue) {
    // Refresh when the URL changes
    showEvents();
  });

  // Initial load.
  showEvents();

  window.setInterval(showEvents, 3000);
}
